import whisper

class WhisperPythonService:
    def __init__(self, model_name = "tiny"):
        self.model = whisper.load_model(model_name)

    def transcribe_file(self,audio_path):
        result = self.model.transcribe(audio_path)
        return result["text"]

# class WhisperCppService: #for mobile platform