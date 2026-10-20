from machine import Pin, PWM
import network
import socket
import time
import gc
from secrets import WIFI_SSID, WIFI_PASSWORD

class Servo:
    def __init__(self, pin):
        self.pwm = PWM(Pin(pin), freq=50)
        self.angle = 90

    def set(self, angle):
        self.angle = max(0, min(180, int(angle)))
        pulse_us = 500 + self.angle / 180 * 1900
        self.pwm.duty(int(pulse_us * 1023 / 20000))

    def off(self):
        self.pwm.duty(0)

servo_a = Servo(4)
servo_b = Servo(5)
servo_c = Servo(6)
servo_d = Servo(7)
servos = [servo_a, servo_b, servo_c, servo_d]

def move_pose(target, speed=2):
    # C軸だけ反転。Android側のC値は論理値のまま送る。
    physical_target = [target[0], target[1], 180 - target[2], target[3]]

    while any(servo.angle != angle for servo, angle in zip(servos, physical_target)):
        for servo, angle in zip(servos, physical_target):
            if servo.angle < angle:
                servo.set(min(angle, servo.angle + 1))
            elif servo.angle > angle:
                servo.set(max(angle, servo.angle - 1))
        time.sleep_ms(max(1, speed) * 8)

def parse_request(request):
    try:
        target = request.split(' ')[1]
        path = target.split('?', 1)[0]
        query = target.split('?', 1)[1] if '?' in target else ''
        params = dict(item.split('=', 1) for item in query.split('&') if '=' in item)
        return path, params
    except Exception:
        return '', {}

def send_response(client, code, body):
    data = body.encode()
    header = (
        'HTTP/1.1 %d OK\r\n'
        'Content-Type: text/plain\r\n'
        'Content-Length: %d\r\n'
        'Connection: close\r\n\r\n'
    ) % (code, len(data))
    client.send(header.encode())
    client.send(data)

wifi = network.WLAN(network.STA_IF)
wifi.active(True)
wifi.connect(WIFI_SSID, WIFI_PASSWORD)

while not wifi.isconnected():
    time.sleep_ms(250)

print('eArm URL: http://' + wifi.ifconfig()[0])
move_pose([90, 120, 90, 35])

server = socket.socket()
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind(('0.0.0.0', 80))
server.listen(4)

while True:
    client, _ = server.accept()

    try:
        request = client.recv(1024).decode()
        path, params = parse_request(request)

        if path == '/health':
            send_response(client, 200, 'OK')

        elif path == '/pose':
            move_pose(
                [
                    int(params.get('a', servo_a.angle)),
                    int(params.get('b', servo_b.angle)),
                    int(params.get('c', 90)),
                    int(params.get('d', servo_d.angle))
                ],
                int(params.get('speed', 2))
            )
            send_response(client, 200, 'OK')

        elif path == '/stop':
            for servo in servos:
                servo.off()
            send_response(client, 200, 'STOPPED')

        else:
            send_response(client, 404, 'NOT FOUND')

    except Exception as error:
        try:
            send_response(client, 500, 'ERROR ' + str(error))
        except Exception:
            pass

    try:
        client.close()
    except Exception:
        pass

    gc.collect()
