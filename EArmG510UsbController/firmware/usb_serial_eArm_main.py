# Save on ESP32-C3 as main.py
from machine import Pin, PWM
import sys,uselect,time
class S:
 def __init__(self,p,a,save=False):self.p=PWM(Pin(p),freq=50);self.a=a;self.d=0;self.save=save;self.last=time.ticks_ms();self.w(a)
 def w(self,a):self.a=max(0,min(180,a));self.p.duty_u16(int((500+self.a/180*1900)/20000*65535));self.last=time.ticks_ms()
 def step(self):
  if self.d:self.w(self.a+self.d*2)
  elif self.save and time.ticks_diff(time.ticks_ms(),self.last)>40000:self.p.duty_u16(0)
s={'A':S(4,90),'B':S(5,120),'C':S(6,60),'D':S(7,90,True)};b=PWM(Pin(9));b.duty_u16(0);q=uselect.poll();q.register(sys.stdin,uselect.POLLIN)
def stop():
 for x in s.values():x.d=0
def cmd(z):
 p=z.strip().upper().split()
 if not p:return
 if p[0]=='PING':print('OK')
 elif p[0]=='STOP':stop();print('OK')
 elif p[0]=='BUZZER' and len(p)>1:b.freq(1000);b.duty_u16(32768 if p[1]=='1' else 0);print('OK')
 elif len(p)>1 and '_' in p[0]:
  a,w=p[0].split('_',1)
  if a in s:s[a].d=({'A':{'MINUS':1,'PLUS':-1},'B':{'MINUS':1,'PLUS':-1},'C':{'MINUS':-1,'PLUS':1},'D':{'MINUS':1,'PLUS':-1}}[a].get(w,0) if p[1]=='1' else 0);print('OK')
print('EARM_USB_READY')
while True:
 if q.poll(0):
  try:cmd(sys.stdin.readline())
  except Exception as e:stop();print('ERR',e)
 for x in s.values():x.step()
 time.sleep_ms(20)