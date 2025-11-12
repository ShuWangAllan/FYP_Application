# coding: UTF-8
# Visible Spoken Language ---- UI ---- Welcome and Info
# using kivy

from kivy.config import Config
Config.set('graphics', 'width', '1100')
Config.set('graphics', 'height', '700')

from kivy.app import App
from kivy.lang import Builder
from kivy.properties import StringProperty, NumericProperty, ListProperty, BooleanProperty
from kivy.clock import Clock
from kivy.uix.screenmanager import ScreenManager, Screen
from kivy.graphics import Color, Line, Rectangle

import random
import math
from collections import deque

KV = r'''
#: import dp kivy.metrics.dp

# share UI bits
<Header@BoxLayout>
    size_hint_y: None
    height: dp(56)
    padding: dp(12), 0
    spacing: dp(12)
    canvas.before:
        Color: rgba: 0.11, 0.11, 0.13, 1
        Rectangle: pos: self.pos; size: self.size
    Label:
        text: app.header_text
        font_size: "18sp"
        bold: True
        color: 1,1,1,1
        halign: "left"; valign: "middle"; text_size: self.size
    Widget:
    Label:
        text: app.status_text
        color: 0.8,0.85,0.9,1
        font_size: "14sp"
        size_hint_x: None
        width: dp(280)
        halign: "right"; valign: "middle"; text_size: self.size

<Footer@BoxLayout>:
    size_hint_y: None
    height: dp(32)
    padding: dp(12), 0
    canvas.before:
        Color: rgba: 0.11, 0.11, 0.13, 1
        Rectangle: pos: self.pos; size: self.size
    Label:
        text: "Mock UI - Kivy"
        color: 0.7,0.75,0.8,1

# Screens
<WelcomeScreen>:
    name: "Welcome"
    BoxLayout:
        orientation: "vertical"
        Header:
        BoxLayout:
            orientation: "vertical"
            padding: dp(40)
            spacing: dp(20)
            canvas.before:
                Color: rgba: 0.06,0.04,0.07,1
                Rectangle: pos: self.pos; size: self.size
                
            Label:
                text: "Visible Spoken Language"
                font_size: "36sp"
                bold: True
                color: 1,1,1,1
                size_hint_y: None
                height: dp(100)
                
            Label:
                text: "让语言学习更具视觉化：通过语音和面部识别，\\n在手机上训练发音、声调与口型。"
                font_size: "18sp"
                color: 0.9,0.92,0.96,1
                halign: "center"; valign: "middle"; text_size: self.size
            
            Widget:
            
            BoxLayout:
                size_hint_y: None
                height: dp(210)
                orientation: "vertical"
                spacing: dp(14)
                Button:
                    text: "开始学习/Start Learning"
                    font_size: "22sp"
                    on_release: app.goto("practice")
                Button:
                    text: "info/功能介绍"
                    font_size: "22sp"
                    on_release: app.goto("info")
                Button:
                    text: "退出/Quit"
                    font_size: "22sp"
                    on_release: app.stop()
            Widget:
        Footer:

<InfoScreen>:
    name: "Info"
    BoxLayout:
        orientation: "vertical"
        padding: dp(28)
        spacing: dp(16)
        canvas.before:
            Color: rgba: 0.06,0.06,0.07,1
            Rectangle: pos: self.pos; size: self.size
        Label:
            text: "功能介绍/Functions Intro"
            bold: True
            font_size: "28sp"
            color: 1,1,1,1
            size_hint_y: None
            height: dp(40)
        Label:
            text:
                "发音练习：实时显示音高曲线和目标四声轮库的偏差\\n" + \
                "口型提示：相机捕捉面部关键点，给出口型反馈\\n" + \
                "学习单元：支持按音节与声调组织\\n" + \
                "进度记录：后续可按成绩与曲线回放"
            color: 0.9,0.92,0.96,1
            halign: "left"; valign: "top"; text_size: self.size
        Widget:
        BoxLayout:
            size_hint_y: None
            height: dp(60)
            spacing: dp(12)
            Button:
                text: "返回"
                on_release: app.back()
            Button:
                text: "练习"
                on_release: app.goto("practice")
    Footer:

<HomeScreen>:
    name: "home"
    BoxLayout:
        orientation: "vertical"
        Header:
        BoxLayout:
            padding: dp(24)
            spacing: dp(24)
            canvas.before:
                Color: rgba: 0.06,0.06,0.07,1
                Rectangle: pos: self.pos; size: self.size
            BoxLayout:
                orientation: "vertical"
                spacing: dp(16)
                Label:
                    text: "Welcame!"
                    font_soze: "26sp"
                    bold: True
                    color: 1,1,1,1
                    size_hint_y: None
                    height: dp(40)
                Label:
                    text: "This is the director of function in this app. Press 'practice' to start"
                    color: 0.9,0.92,0.96,1
                BoxLayout:
                    size_hint_y: None
                    height: dp(56)
                    spacing: dp(12)
                    Button:
                        text: "Settings (placeholder)"
                        disable: True
            Widget:
        Footer:


<PracticeScreen>:
    name: "practice"
    BoxLayout:
        orientation: "vertical"
        Header:
        BoxLayout:
            padding: dp(16)
            spacing: dp(16)
            canvas.before:
                Color: rgba: 0.06,0.06,0.07,1
                Rectangle: pos: self.pos; size: self.size
                
            # Left: Camera placeholder
            BoxLayout:
                orientation: "vertical"
                size_hint_x: 0.46
                spacing: dp(10)
                canvas.before:
                    Color: rgba: 0.12,0.12,0.14,1
                    Rectangle: pos: self.pos; size: self.size
                Label:
                    text: "Camera Preview(placeholder)"
                    color: 1,1,1,1
                    size_hint_y: None
                    height: dp(30)
                Widget:
                    id: camera_box
                    canvas:
                        Color: rgba: 0.15,0.15,0.18,1
                        Rectangle: pos: self.pos; size: self.size
                        Color: rgba: 0.35,0.35,0.45,1
                        Line: rectangle: (*self.pos, *self.size); width: 1.2
                BoxLayout:
                    size_hint_y: None
                    height: dp(56)
                    spacing: dp(10)
                    TextInput:
                        id: syllable_input
                        hint_text: "Syllable (e.g., 'ma')"
                        multiline: False
                    Spinner:
                        id: tone_spinner
                        text: "Tone 1"
                        values: ["Tone 1", "Tone 2", "Tone 3", "Tone 4"]
                    ToggleButton:
                        id: start_btn
                        text: "Start"
                        on_state: root.on_toggle(self.state)
                        
            # Right: Feedback + plot
            BoxLayout:
                orientation: "vertical"
                size_hint_x: 0.54
                spacing: dp(10)
                canvas.before:
                    Color: rgba: 0.12,0.12,0.14,1
                    Rectangle: pos: self.pos; size: self.size
                    
                BoxLayout:
                    size_hint_y: None
                    height: dp(64)
                    padding: dp(10)
                    spacing: dp(10)
                    Label:
                        text: "Feedback"
                        color: 1,1,1,1
                        size_hint_x: None
                        width: dp(100)
                    Label:
                        id: feedback_label
                        text: root.feedback_text
                        color: root.feedback_color
                        bold: True
                        font_size: "20sp"
                    
                BoxLayout:
                    orientation: "vertical"
                    padding: dp(10)
                    spacing: dp(8)
                    Label:
                        text: "Pitch Contour (mock) vs Target"
                        color: 0.9,0.92,0.96,1
                        size_hint_y: None
                        height: dp(28)
                    PitchPanel:
                        id: pitch_panel
                        size_hint_y: 0.85
                        
                BoxLayout:
                    size_hint_y: None
                    height: dp(56)
                    spacing: dp(12)
                    Button:
                        text: "返回/Back"
                        on_release: app.back()
                    Button:
                        text: "重置/Reset"
                        on_release: root.reset()
                        
<PitchPanel@Widget>:
    # Draw rolling live pitch and the target curve
    target_points: []
    live_points: []
    def on_size(self, *args): self.canvas.ask_update()
    def on_pos(self, *args): self.canvas.ask_update()
    def on_target_points(self, *args): self.canvas.ask_update()
    def on_live_points(self, *args): self.canvas.ask_update()
    canvas:
        Color: rgba: 0.08,0.08,0.1,1
        Rectangle: pos: self.pos; size: self.size
        Color: rgba: 0.25,0.25,0.3,1
        Line: rectangle: (*self.pos, *self.size); width: 1.2
        Color: rgba: 0.6,0.6,0.75,1
        Line: points: self.target_points; width: 1.6; cap: "round"; joint: "round"
        Color: rgba: 0.95,0.995,0.95,1
        Line: points: self.live_points; width: 2.0; cap: "round"; joint: "round"
        
ScreenManager:
    WelcomeScreen:
    InfoScreen:
    HomeScreen:
    PracticeScreen:
'''

class WelcomeScreen(Screen):
    pass

class InfoScreen(Screen):
    pass

class HomeScreen(Screen):
    pass

class PracticeScreen(Screen):
    feedback_text = StringProperty("Idle")
    feedback_color = ListProperty([0.8, 0.85, 0.9, 1])
    running = BooleanProperty(False)

    live_buf = deque(maxlen= 180) # rolling buffer for mock pitch(0..1)
    t = 0.0

    def on_pre_enter(self, *args):
        self.set_target_from_spinner()
        self.feedback_text = "Idle"
        self.feedback_color = [0.8, 0.85, 0.9, 1]

    def on_toggle(self, state):
        if state == "down":
            self.ids.start_btn.text = "Stop"
            self.running = True
            self.live_buf.clear()
            self.t = 0.0
            self.feedback_text = "Listening..."
            self.feedback_color = [0.9, 0.92, 0.96, 1]
            self.set_target_from_spinner()
            self._event = Clock.schedule_interval(self._update_sim, 1/30)
        else:
            self.ids.start_btn.text = "Start"
            self.running = False
            if hasattr(self, "_event"):
                self._event.cancel()
            self.feedback_text = "Stopped"
            self.feedback_color = [0.7, 0.75, 0.8, 1]
    def reset(self):
        if self.running and hasattr(self, "_event"):
            self._event.cancel()
        self.running = False
        self.ids.start_btn.state = "normal"
        self.ids.start_btn.text = "Start"
        self.live_buf.clear()
        self.feedback_text = "Idle"
        self.feedback_color = [0.8, 0.85, 0.9, 1]

    def set_target_from_spinner(self):
        spinner_text = self.ids.tone_spinner.text
        tone = int(spinner_text.split()[-1])
        self._target_curve = self._gen_target_curve(tone)
        self._update_target_points()

    def _gen_target_curve(self, tone):
        # 0..1 normalized time -> 180ms
        n = 180
        xs = [i / (n - 1) for i in range(n)]
        if tone == 1: #高平
            ys = [0.8 for _ in xs]
        elif tone == 2: #上升
            ys = [0.35 + 0.5 * x for x in xs]
        elif tone == 3: # 降升
            ys = [0.7 - 0.4 * math.sin(math.pi*x) for x in xs]
        else: # 下降
            ys = [0.85 - 0.6*x for x in xs]
        return ys

    def _update_target_points(self):
        panel = self.ids.pitch_panel
        W,H = panel.width, panel.height
        x0, y0 = panel.x, panel.y
        n = len(self._target_curve)
        if n < 2 or W <= 0 or H <= 0:
            panel.target_points = []
            return
        pts = []
        for i, y in enumerate(self._target_curve):
            x = x0 + (i / (n - 1)) * W
            py = y0 + y * H
            pts += [x, py]
        panel.target_points = pts

    def _update_live_points(self):
        panel = self.ids.pitch_panel
        W, H = panel.width, panel.height
        x0, y0 = panel.x, panel.y
        n = len(self.live_buf)
        if n < 2 or W <= 0 or H <= 0:
            panel.live_point = []
            return
        pts = []
        for i, y in enumerate(list(self.live_buf)):
            x = x0 + (i / max(1 , n-1)) * W
            py = y0 + y * H
            pts += [x, py]
        panel.live_point = pts

    def _update_sim(self, dt):
        # Simulate pitch following target with noise & drift
        self.t += dt
        idx = min(len(self.live_but), len(self._target_curve) - 1)
        target_y = self._target_curve[idx] if len(self._target_curve) else 0.5
        noise = random.uniform(-0.05, 0.05)
        drift = 0.03 * math.sin(2 * math.pi * 0.4 * self.t)
        y = max(0.0, min(1.0, target_y + noise + drift))
        self.live_buf.append(y)

        # Update lines
        self._update_live_points()
        self._update_target_points()

        # Feedback via MAE over last 60 samples
        window = list(self.live_buf)[-60:]
        if window and len(window) <= len(self._target_curve):
            start = len(self.live_buf) - len(window)
            target_window = self._target_curve[start:start + len(window)]
            mae = sum(abs(a - b) for a, b in zip(window, target_window)) / len(window)
        else:
            mae = 1.0

        if mae < 0.05:
            self.feedback_text = "Excellent - veryu close"
            self.feedback_color = [0.2, 0.9, 0.4, 1]
        elif mae < 0.12:
            self.feedback_text = "Good - minor deviation"
            self.feedback_color = [0.85, 0.85, 0.2, 1]
        else:
            self.feedback_text = "Needs work - off target"
            self.feedback_color = [0.95, 0.35, 0.35, 1]

class Root(ScreenManager):
    pass

class VisibleSpokenLanguageApp(App):
    header_text = StringProperty("Visible Spoken Language - Prototype")
    statue_text = StringProperty("Ready")
    _history = []

    def build(self):
        self.title = "Visible Spoken Language - Prototype"
        root = Builder.load_string(KV)

        # defult in welcome page
        root.current = "welcome"
        self._history.append("welcome")
        return root

    def goto(self, screen_name: str):
        sm: ScreenManager = self.root
        if sm.current != screen_name:
            self._history.append(screen_name)
            sm.current = screen_name

    def back(self):
        if len(self._history) > 1:
            self._history.pop() # current
            target = self._history.pop() # previous
            self.goto(target)
        else:
            self.root.current = "welcome"

if __name__ == "__main__":
    VisibleSpokenLanguageApp().run()