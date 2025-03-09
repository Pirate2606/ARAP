from flask import Flask, request, send_file, jsonify
import os
import time

app = Flask(__name__)

PRE_SAVED_AUDIO_FILE = "static/recording_1.mp3"
UPLOAD_FOLDER = "static"

@app.route("/process_question", methods=["POST"])
def process_question():
    data = request.get_json()

    if not data or "question" not in data:
        return jsonify({"error": "Missing 'question' field"}), 400

    question = data["question"]
    print(f"Received question: {question}")

    if not os.path.exists(PRE_SAVED_AUDIO_FILE):
        return jsonify({"error": "Audio file not found"}), 500

    time.sleep(5)

    return send_file(PRE_SAVED_AUDIO_FILE, mimetype="audio/mpeg", as_attachment=True, download_name="response_audio.mp3")


@app.route("/final_response", methods=["POST"])
def final_response():
    question = request.form.get("question")
    file = request.files.get("file")

    if not question:
        return jsonify({"error": "Missing 'question' field"}), 400
    
    if not file:
        return jsonify({"error": "No file provided"}), 400

    file_path = os.path.join(UPLOAD_FOLDER, file.filename)
    file.save(file_path)

    print(f"Received question: {question}")
    print(f"Saved audio file to: {file_path}")

    return jsonify({"message": "File uploaded successfully", "file_path": file_path}), 200

    


if __name__ == "__main__":
    app.run(port=5001, debug=True)
