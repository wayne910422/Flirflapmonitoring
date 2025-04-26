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
app = Flask(__name__)
CORS(app, resources={r"/*": {"origins": "*"}})

# 儲存目錄與資料庫（儲存功能用）
BASE_DIR = "C:/xampp/htdocs/thermal_image_web/uploads/"
DB_PATH = "results.db"

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
        patientID = request.form.get('patientID', 'unknown')
        skinLabel = request.form.get('skinLabel', '')
        flapTemp_str = request.form.get('flapTemp', '')
        controlTemp_str = request.form.get('controlTemp', '')
        if not flapTemp_str or not controlTemp_str:
            return jsonify({'error': '請提供 Flap 與 Control 溫度'}), 400
        flapTemp = float(flapTemp_str)
        controlTemp = float(controlTemp_str)
        handwrittenTemp_str = f"Flap: {flapTemp}, Control: {controlTemp}"

        regionNormal1Str = request.form.get('regionNormal1', '')
        regionNormal2Str = request.form.get('regionNormal2', '')
        regionNormal1 = parse_region(regionNormal1Str)
        regionNormal2 = parse_region(regionNormal2Str)
        regionUnion = compute_region_union(regionNormal1, regionNormal2)

        visible_file = request.files.get('visibleImage')
        visible_bytes = np.frombuffer(visible_file.read(), np.uint8)
        visible_img = cv2.imdecode(visible_bytes, cv2.IMREAD_UNCHANGED)

        nx1, ny1, nx2, ny2 = map(int, regionUnion)
        visible_region = visible_img[ny1:ny2, nx1:nx2]

        flap_rgb = compute_region_average(visible_img, regionNormal1)
        control_rgb = compute_region_average(visible_img, regionNormal2)
        overall_avg = np.mean(visible_region, axis=(0,1)).tolist()
        rgb_diff = (np.array(flap_rgb) - np.array(control_rgb)).tolist()
        tempdif = flapTemp - controlTemp

        # 特徵輸入
        x_8 = [[
            flap_rgb[0], flap_rgb[1], flap_rgb[2],
            control_rgb[0], control_rgb[1], control_rgb[2],
            flapTemp, controlTemp
        ]]
        x_4 = [[tempdif, rgb_diff[0], rgb_diff[1], rgb_diff[2]]]

        with open('model.pkl','rb') as f:
            loaded_models = pickle.load(f)

        predictions = {}
        probabilities = {}
        model_types = {}

        model_groups = ['KNN', 'DT', 'RF', 'AD']
        for algo in model_groups:
            for i in [1, 2, 3, 4]:
                model_key = f"{algo}{i}"
                model = loaded_models.get(model_key)
                if not model:
                    continue
                model_types[f"{model_key}"] = type(model).__name__

                if i in [1, 3]:
                    x_input = x_8
                else:
                    scaler_key = f"scmodel{i}"
                    scaler = loaded_models.get(scaler_key, StandardScaler())
                    x_input = scaler.transform(x_4)

                y_pred = model.predict(x_input)
                y_prob = model.predict_proba(x_input)

                label = ['Normal Flap', 'Arterial insufficiency', 'Venous insufficiency'][y_pred[0]]
                predictions[model_key] = label
                probabilities[model_key] = {
                    'Normal Flap': '{:.1%}'.format(y_prob[0,0]),
                    'Arterial insufficiency': '{:.1%}'.format(y_prob[0,1]),
                    'Venous insufficiency': '{:.1%}'.format(y_prob[0,2])
                }

        return jsonify({
            'flapTemp': flapTemp,
            'controlTemp': controlTemp,
            'temp_diff': tempdif,
            'flap_rgb': flap_rgb,
            'control_rgb': control_rgb,
            'overall_avg': overall_avg,
            'rgb_diff': rgb_diff,
            'handwrittenTemp': handwrittenTemp_str,
            'predictions': predictions,
            'model_probabilities': probabilities,
            'model_types': model_types
        })

    except Exception as e:
        print(f"Error: {str(e)}")
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
    target_dir = os.path.join(r"C:\xampp\htdocs\thermal_image_web", skinLabel)
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
    target_dir = os.path.join(r"C:\xampp\htdocs\thermal_image_web", skinLabel)
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
    app.run(debug=True, host='127.0.0.1', port=5000)
