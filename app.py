from flask import Flask, request, jsonify, send_file
from flask_cors import CORS
import cv2
import numpy as np
import os
from datetime import datetime
from io import BytesIO
import sqlite3
import json
import pickle
import pandas as pd
from imblearn.combine import SMOTETomek
from sklearn.preprocessing import StandardScaler
import traceback
app = Flask(__name__)
CORS(app, resources={r"/*": {"origins": "*"}})

# 儲存目錄與資料庫（儲存功能用）
BASE_DIR = "/home/ec2-user/gdrive1/uploads/"
DB_PATH = "/home/ec2-user/gdrive1/results.db"

def ensure_directory_exists(path):
    try:
        if not os.path.exists(path):
            os.makedirs(path, exist_ok=True)
        if not os.access(path, os.W_OK):
            raise PermissionError(f"無權限寫入 {path}")
        return True
    except Exception as e:
        print(f"Error ensuring directory {path}: {str(e)}")
        return False

def parse_region(region_str):
    try:
        if not region_str or not region_str.strip():
            return None
        arr = region_str.split(',')
        if len(arr) != 4:
            return None
        return list(map(float, arr))
    except Exception:
        return None

def compute_region_union(region1, region2):
    if not region1 or not region2:
        return None
    return [min(region1[0], region2[0]), min(region1[1], region2[1]),
            max(region1[2], region2[2]), max(region1[3], region2[3])]

def compute_region_average(img, region):
    # img 為 OpenCV 影像（BGR），轉回 RGB 輸出
    x1, y1, x2, y2 = map(int, region)
    sub_img = img[y1:y2, x1:x2]
    if sub_img.size == 0:
        return None
    bgr_mean = np.mean(sub_img, axis=(0, 1)).tolist()
    return [bgr_mean[2], bgr_mean[1], bgr_mean[0]]

def get_timestamp():
    return datetime.now().strftime("%Y%m%d%H%M%S")

# 以下資料庫相關函數與儲存功能僅供 /save_label 與 /save_visible 使用
def init_db():
    try:
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        c.execute('''
            CREATE TABLE IF NOT EXISTS ImageResults (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                patientID TEXT,
                skinLabel TEXT,
                handwrittenTemp TEXT,
                regionThermal1 TEXT,
                regionThermal2 TEXT,
                regionNormal1 TEXT,
                regionNormal2 TEXT,
                average_rgb TEXT,
                thermalRegion1Avg TEXT,
                thermalRegion2Avg TEXT,
                normalRegion1Avg TEXT,
                normalRegion2Avg TEXT,
                thermalFile TEXT,
                visibleFile TEXT,
                combinedFile TEXT,
                created_at TEXT
            )
        ''')
        conn.commit()
        conn.close()
        print("Database initialized.")
    except Exception as e:
        print("Database initialization error:", str(e))

def store_result_to_db(patientID, skinLabel, handwrittenTemp, regionThermal1Str, regionThermal2Str,
                       regionNormal1Str, regionNormal2Str, average_rgb, thermalRegion1Avg, thermalRegion2Avg,
                       normalRegion1Avg, normalRegion2Avg, thermalFile, visibleFile, combinedFile):
    try:
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        created_at = get_timestamp()
        c.execute('''
            INSERT INTO ImageResults (
                patientID, skinLabel, handwrittenTemp, regionThermal1, regionThermal2,
                regionNormal1, regionNormal2, average_rgb, thermalRegion1Avg, thermalRegion2Avg,
                normalRegion1Avg, normalRegion2Avg, thermalFile, visibleFile, combinedFile, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (
            patientID, skinLabel, handwrittenTemp, regionThermal1Str, regionThermal2Str,
            regionNormal1Str, regionNormal2Str,
            json.dumps(average_rgb),
            None, None,
            None, None,
            "", "", "", created_at
        ))
        conn.commit()
        conn.close()
        print("Result stored in database.")
    except Exception as e:
        print("Database error:", str(e))

# 呼叫資料庫初始化（供儲存功能使用）
init_db()

# 全域變數（區域影像下載用，這裡不影響分析功能）
region_images = {}

# 分析路由：按下分析按鈕後直接從上傳的檔案中進行分析，不儲存也不合併影像
@app.route('/upload', methods=['POST'])
def upload():
    try:
        # 溫度讀取與檢查
        flapTemp_str = request.form.get('flapTemp', '')
        controlTemp_str = request.form.get('controlTemp', '')
        if not flapTemp_str or not controlTemp_str:
            return jsonify({'error': '請提供 flapTemp 與 controlTemp'}), 400

        flapTemp = float(flapTemp_str)
        controlTemp = float(controlTemp_str)

        # 區域座標解析與檢查
        region1_str = request.form.get('regionNormal1', '')
        region1 = parse_region(region1_str)
        if not region1:
            return jsonify({'error': 'regionNormal1 格式錯誤，需為 x1,y1,x2,y2'}), 400

        # 圖片處理與檢查
        visible_file = request.files.get('visibleImage')
        if visible_file is None:
            return jsonify({'error': '缺少 visibleImage 圖片檔案'}), 400

        visible_bytes = np.frombuffer(visible_file.read(), np.uint8)
        visible_img = cv2.imdecode(visible_bytes, cv2.IMREAD_UNCHANGED)
        if visible_img is None:
            return jsonify({'error': '無法解碼圖片，請確認格式正確'}), 400

        # 擷取 flap 區域 RGB 平均值
        flap_rgb = compute_region_average(visible_img, region1)
        if flap_rgb is None:
            return jsonify({'error': 'RGB 擷取失敗，請確認座標區域是否正確'}), 400

        # 特徵構建，欄位名稱必須與訓練時完全相符
        x_df = pd.DataFrame([[flapTemp, flap_rgb[0], flap_rgb[1], flap_rgb[2]]],
                            columns=['Flap Temperature', 'Flap_R', 'Flap_G', 'Flap_B'])

        # 載入模型與標準化器
        with open('model.pkl', 'rb') as f:
            loaded_models = pickle.load(f)

        RF2 = loaded_models.get('RF2')
        scaler = loaded_models.get('scmodel2')
        if RF2 is None:
            return jsonify({'error': 'RF2 模型載入失敗'}), 500
        if scaler is None:
            return jsonify({'error': 'scmodel2 載入失敗'}), 500

        # 預測
        x_input = scaler.transform(x_df)
        y_pred = RF2.predict(x_input)
        y_prob = RF2.predict_proba(x_input)

        label = ['Normal Flap', 'Arterial insufficiency', 'Venous insufficiency'][y_pred[0]]

        return jsonify({
            'flapTemp': flapTemp,
            'controlTemp': controlTemp,
            'flap_rgb': flap_rgb,
            'prediction': label,
            'probabilities': {
                'Normal Flap': '{:.1%}'.format(y_prob[0, 0]),
                'Arterial insufficiency': '{:.1%}'.format(y_prob[0, 1]),
                'Venous insufficiency': '{:.1%}'.format(y_prob[0, 2]),
            },
            'model_used': 'RF2'
        })

    except Exception as e:
        print(f"Error: {str(e)}")
        traceback.print_exc()
        return jsonify({'error': str(e)}), 500


    
 
    
    
@app.route('/save_label', methods=['POST'])
def save_label():
    patientID = request.form.get('patientID')
    skinLabel = request.form.get('skinLabel')
    file = request.files.get('thermalImage')
    if not patientID or not skinLabel or file is None:
        return jsonify({"error": "缺少必需的欄位（patientID、skinLabel 或 thermalImage）"}), 400
    valid_labels = ["正常皮膚", "缺血性皮膚", "瘀血性皮膚"]
    if skinLabel not in valid_labels:
        return jsonify({"error": "無效的皮膚 Label 值"}), 400
    target_dir = os.path.join(r"/home/ec2-user/gdrive1/uploads", skinLabel)
    os.makedirs(target_dir, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
    filename = f"{patientID}_{timestamp}_{skinLabel}.jpg"
    file_path = os.path.join(target_dir, filename)
    if os.path.exists(file_path):
        base_name = f"{patientID}_{timestamp}_{skinLabel}"
        counter = 1
        while os.path.exists(file_path):
            filename = f"{base_name}_{counter}.jpg"
            file_path = os.path.join(target_dir, filename)
            counter += 1
    try:
        file.save(file_path)
    except Exception as e:
        return jsonify({"error": f"儲存檔案失敗: {e}"}), 500
    return jsonify({"message": "Label 儲存成功", "path": file_path})

@app.route('/save_visible', methods=['POST'])
def save_visible():
    patientID = request.form.get('patientID')
    skinLabel = request.form.get('skinLabel')
    file = request.files.get('visibleImage')
    if not patientID or not skinLabel or file is None:
        return jsonify({"error": "缺少必需的欄位（patientID、skinLabel 或 visibleImage）"}), 400
    valid_labels = ["正常皮膚", "缺血性皮膚", "瘀血性皮膚"]
    if skinLabel not in valid_labels:
        return jsonify({"error": "無效的皮膚 Label 值"}), 400
    target_dir = os.path.join(r"/home/ec2-user/gdrive1/uploads", skinLabel)
    os.makedirs(target_dir, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
    filename = f"{patientID}_{timestamp}_{skinLabel}.jpg"
    file_path = os.path.join(target_dir, filename)
    if os.path.exists(file_path):
        base_name = f"{patientID}_{timestamp}_{skinLabel}"
        counter = 1
        while os.path.exists(file_path):
            filename = f"{base_name}_{counter}.jpg"
            file_path = os.path.join(target_dir, filename)
            counter += 1
    try:
        file.save(file_path)
    except Exception as e:
        return jsonify({"error": f"儲存檔案失敗: {e}"}), 500
    return jsonify({"message": "可見光影像儲存成功", "path": file_path})

@app.route('/download_region', methods=['GET'])
def download_region():

    try:
        region_type = request.args.get('type', '').lower()
        region_num = int(request.args.get('region', '1'))
        if region_type not in ['thermal', 'visible'] or region_num not in [1, 2]:
            return jsonify({'error': '無效的區域類型或編號'}), 400
        region_key = f"{region_type}{region_num}"
        if region_key not in region_images:
            return jsonify({'error': '區域影像未找到，請先上傳並分析'}), 400
        region_img = region_images[region_key]
        _, buffer = cv2.imencode('.jpg', region_img)
        byte_io = BytesIO(buffer)
        return send_file(
            byte_io,
            mimetype='image/jpg',
            as_attachment=True,
            download_name=f'{region_type}_region{region_num}_{get_timestamp()}.jpg'
        )
    except Exception as e:
        print(f"Error downloading region: {str(e)}")
        return jsonify({'error': str(e)}), 500

@app.errorhandler(404)
def not_found_error(error):
    return jsonify({'error': 'Not found'}), 404

@app.errorhandler(500)
def internal_server_error(error):
    return jsonify({'error': 'Internal server error'}), 500


if __name__ == '__main__':
    app.run(host="0.0.0.0", port=5000, debug=True)

