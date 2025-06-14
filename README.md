20241229 Discussion log:
1. 初步程式功能需求:
- 固定間隔20分鐘擷取熱影像+一般照片(手機相機)影像，存於local端
- 檔案命名: 手動輸入之病歷號_timestamp_(flir)or(cam)
- 從熱影像擷取溫度資料(需要兩個測溫點)，及手機影像擷取兩點之RGB data
- 整合前次研究之Random Forrest之python code，生成model analysis之結果
- 目標: 三月底

2. 硬軟體整合:
- "Flirwireless " Sample code 含有之功能: 熱影像串流 / 參數與顯示調整
- Github平台上整合

3. 系統平台與部署策略:
- 平台選擇與方向調整  
- 原本規劃開發成手機應用程式（App），但經評估後考量到後續的維護便利性與跨平台兼容性，決定改為開發基於瀏覽器的網站系統。網站平台具有較佳的擴充性與遠端維運優勢，並能更有效整合雲端資源。  
- 分析功能：  
- 使用者上傳影像資料（熱影像與一般照片）後，系統會自動擷取指定點位的溫度與RGB資訊，並結合Random Forest模型進行分析，輸出model analysis之結果與檔案命名。
測試附圖:

![image](https://github.com/user-attachments/assets/b012aa39-aacd-4f7d-929f-7a68a90b7c83)

指定點位附圖:

![image](https://github.com/user-attachments/assets/a10400c1-c139-4c44-bbc1-a08dd2b1270a)

4. AWS 上架與部署:
- 將整體網站部署於 AWS 雲端平台，採用以下架構：  
- EC2 虛擬伺服器：作為網站主機，負責處理使用者請求、資料處理與模型分析等後端運算。  
- Route 53 網域管理：設定專屬網域名稱，供使用者透過網址連線網站。

5.使用者介面與視覺化:
- 設計簡易網頁操作介面，使用者可上傳影像資料並查看分析結果
- 指定點位由滑鼠或是觸控螢幕去定位兩區(x,y)值

6.安全性與資料儲存:
- 整體資料傳輸透過HTTPS確保安全性
- EC2 掛載GOOGLE雲端硬碟 使病例直接分類的最後的存儲位置

分析附圖:
![image](https://github.com/user-attachments/assets/a6776bee-5e1f-4b0b-b51e-8ce7f39391f2)


