# Run on a PC with Python and Ultralytics installed.
# pip install ultralytics tensorflow
from ultralytics import YOLO
model = YOLO("yolov8n.pt")  # replace with your custom best.pt
model.export(format="tflite", imgsz=640, int8=False)
# Rename the float32 output to yolov8n_float32.tflite and copy into app/src/main/assets/
