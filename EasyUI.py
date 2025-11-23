# -*- coding: utf-8 -*-
# 简化版 Visible Spoken Language UI
# 只包含：Welcome / Info / Practice
# 没有 PitchPanel，自然也没有 target_points 报错

from kivy.config import Config
Config.set('graphics', 'width', '1100')
Config.set('graphics', 'height', '700')

from kivy.app import App
from kivy.lang import Builder
from kivy.properties import StringProperty, BooleanProperty
from kivy.uix.screenmanager import ScreenManager, Screen

KV = r"""
#:import dp kivy.metrics.dp

<Header@BoxLayout>:
    size_hint_y: None
    height: dp(56)
    padding: dp(12), 0
    spacing: dp(12)
    canvas.before:
        Color:
            rgba: 0.11, 0.11, 0.13, 1
        Rectangle:
            pos: self.pos
            size: self.size
    Label:
        text: app.header_text
        font_size: "18sp"
        color: 1, 1, 1, 1
        halign: "left"
        valign: "middle"
        text_size: self.size
    Widget:
    Label:
        text: app.status_text
        color: 0.8, 0.85, 0.9, 1
        font_size: "14sp"
        size_hint_x: None
        width: dp(240)
        halign: "right"
        valign: "middle"
        text_size: self.size

<Footer@BoxLayout>:
    size_hint_y: None
    height: dp(32)
    padding: dp(12), 0
    canvas.before:
        Color:
            rgba: 0.11, 0.11, 0.13, 1
        Rectangle:
            pos: self.pos
            size: self.size
    Label:
        text: "Mock UI · Kivy"
        color: 0.7, 0.75, 0.8, 1

# ---------------- Welcome 启动页 ----------------
<WelcomeScreen>:
    name: "welcome"
    BoxLayout:
        orientation: "vertical"
        Header:
        BoxLayout:
            orientation: "vertical"
            padding: dp(40)
            spacing: dp(20)
            canvas.before:
                Color:
                    rgba: 0.06, 0.06, 0.07, 1
                Rectangle:
                    pos: self.pos
                    size: self.size

            Label:
                text: "Visible Spoken Language"
                font_size: "36sp"
                color: 1, 1, 1, 1
                size_hint_y: None
                height: dp(100)

            Label:
                text: "让语言学习更具视觉化：通过语音与面部识别，\\n在手机上训练发音、声调与口型。"
                font_size: "18sp"
                color: 0.9, 0.92, 0.96, 1
                halign: "center"
                valign: "middle"
                text_size: self.size

            Widget:

            BoxLayout:
                size_hint_y: None
                height: dp(210)
                orientation: "vertical"
                spacing: dp(14)
                Button:
                    text: "开始学习"
                    font_size: "22sp"
                    on_release: app.goto("practice")
                Button:
                    text: "功能介绍"
                    font_size: "22sp"
                    on_release: app.goto("info")
                Button:
                    text: "退出"
                    font_size: "22sp"
                    on_release: app.stop()

            Widget:
        Footer:

# ---------------- Info 功能介绍页 ----------------
<InfoScreen>:
    name: "info"
    BoxLayout:
        orientation: "vertical"
        Header:
        BoxLayout:
            orientation: "vertical"
            padding: dp(28)
            spacing: dp(16)
            canvas.before:
                Color:
                    rgba: 0.06, 0.06, 0.07, 1
                Rectangle:
                    pos: self.pos
                    size: self.size
            Label:
                text: "功能介绍"
                font_size: "28sp"
                color: 1, 1, 1, 1
                size_hint_y: None
                height: dp(40)
            Label:
                text:
                    "• 发音练习：实时显示音高曲线与目标四声轮廓的偏差（后续接入）\\n" + \
                    "• 口型提示：相机捕捉面部关键点，给出口形反馈（后续接入）\\n" + \
                    "• 学习单元：按音节与声调组织练习（后续接入）\\n" + \
                    "• 进度记录：保存每次练习结果与曲线（后续接入）"
                color: 0.9, 0.92, 0.96, 1
                halign: "left"
                valign: "top"
                text_size: self.size
            Widget:
            BoxLayout:
                size_hint_y: None
                height: dp(60)
                spacing: dp(12)
                Button:
                    text: "返回"
                    on_release: app.back()
                Button:
                    text: "去练习"
                    on_release: app.goto("practice")
        Footer:

# Practice
<PracticeScreen>:
    name: "practice"
    BoxLayout:
        orientation: "vertical"
        Header:
        BoxLayout:
            padding: dp(16)
            spacing: dp(16)
            canvas.before:
                Color:
                    rgba: 0.06, 0.06, 0.07, 1
                Rectangle:
                    pos: self.pos
                    size: self.size

            # 左侧：摄像头占位
            BoxLayout:
                orientation: "vertical"
                size_hint_x: 0.46
                spacing: dp(10)
                canvas.before:
                    Color:
                        rgba: 0.12, 0.12, 0.14, 1
                    Rectangle:
                        pos: self.pos
                        size: self.size
                Label:
                    text: "Camera Preview (占位)"
                    color: 1, 1, 1, 1
                    size_hint_y: None
                    height: dp(30)
                BoxLayout:
                    id: camera_box
                    canvas.before:
                        Color:
                            rgba: 0.15, 0.15, 0.18, 1
                        Rectangle:
                            pos: self.pos
                            size: self.size
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

            # 右侧：反馈 + 图表占位
            BoxLayout:
                orientation: "vertical"
                size_hint_x: 0.54
                spacing: dp(10)
                canvas.before:
                    Color:
                        rgba: 0.12, 0.12, 0.14, 1
                    Rectangle:
                        pos: self.pos
                        size: self.size

                BoxLayout:
                    size_hint_y: None
                    height: dp(64)
                    padding: dp(10)
                    spacing: dp(10)
                    Label:
                        text: "Feedback:"
                        color: 1, 1, 1, 1
                        size_hint_x: None
                        width: dp(90)
                    Label:
                        text: root.feedback_text
                        color: 0.3, 0.9, 0.5, 1
                        font_size: "20sp"

                BoxLayout:
                    orientation: "vertical"
                    padding: dp(10)
                    spacing: dp(8)
                    Label:
                        text: "Pitch Contour (占位示意)"
                        color: 0.9, 0.92, 0.96, 1
                        size_hint_y: None
                        height: dp(24)
                    BoxLayout:
                        id: graph_box
                        canvas.before:
                            Color:
                                rgba: 0.15, 0.15, 0.18, 1
                            Rectangle:
                                pos: self.pos
                                size: self.size
                        Label:
                            text: "将来这里画音高曲线 / 目标曲线"
                            color: 0.8, 0.82, 0.9, 1

                BoxLayout:
                    size_hint_y: None
                    height: dp(56)
                    spacing: dp(12)
                    Button:
                        text: "返回"
                        on_release: app.back()
                    Button:
                        text: "重置"
                        on_release: root.reset()

ScreenManager:
    WelcomeScreen:
    InfoScreen:
    PracticeScreen:
"""

class WelcomeScreen(Screen):
    pass

class InfoScreen(Screen):
    pass

class PracticeScreen(Screen):
    feedback_text = StringProperty("Idle")
    running = BooleanProperty(False)

    def on_pre_enter(self):
        self.feedback_text = "Idle"

    def on_toggle(self, state):
        if state == "down":
            self.ids.start_btn.text = "Stop"
            self.running = True
            self.feedback_text = "Listening..."
        else:
            self.ids.start_btn.text = "Start"
            self.running = False
            self.feedback_text = "Stopped"

    def reset(self):
        self.running = False
        self.ids.start_btn.state = "normal"
        self.ids.start_btn.text = "Start"
        self.feedback_text = "Idle"

class Root(ScreenManager):
    pass

class VisibleSpokenLanguageApp(App):
    header_text = StringProperty("Visible Spoken Language — Prototype")
    status_text = StringProperty("Ready")
    _history = []

    def build(self):
        self.title = "Visible Spoken Language — Prototype"
        root = Builder.load_string(KV)
        root.current = "welcome"
        self._history = ["welcome"]
        return root

    def goto(self, screen_name: str):
        sm = self.root
        if sm.current != screen_name:
            self._history.append(screen_name)
            sm.current = screen_name

    def back(self):
        if len(self._history) > 1:
            self._history.pop()          # 当前
            target = self._history.pop() # 上一个
            self.goto(target)
        else:
            self.root.current = "welcome"

if __name__ == "__main__":
    VisibleSpokenLanguageApp().run()
