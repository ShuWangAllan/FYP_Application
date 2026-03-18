from whisper_service import WhisperPythonService

service = WhisperPythonService("tiny")
text = service.transcribe_file("test.wav")
print(text)