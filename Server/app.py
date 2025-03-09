from flask import Flask, request, jsonify
import csv
import requests
import subprocess
import threading
import time
import os

app = Flask(__name__)

CSV_FILE_PATH = "static/data.csv"
QUESTION_SERVER_URL = "http://127.0.0.1:5001/process_question"
ADB_PLAY_AUDIO_BROADCAST = 'am broadcast -a com.ss.arap.PLAY_AUDIO --es filePath "/storage/emulated/0/Android/data/com.ss.arap/files/question_audio.mp3"'
ADB_START_RECORD_AUDIO_BROADCAST = 'am broadcast -a com.ss.arap.START_RECORDING --es fileName "recording_1.mp3"'
ADB_STOP_RECORT_AUDIO_BROADCAST = 'am broadcast -a com.ss.arap.STOP_RECORDING'
LOGCAT_FILTER = "ARAP"
AUDIO_PUSH_PATH = "/storage/emulated/0/Android/data/com.ss.arap/files/question_audio.mp3"
AUDIO_PULL_PATH = "/storage/emulated/0/Android/data/com.ss.arap/files/recording_1.mp3"
AUDIO_UPLOAD_SERVER_URL = "http://127.0.0.1:5001/final_response"


def process_csv():
    with open(CSV_FILE_PATH, "r") as file:
        reader = csv.DictReader(file)
        for row in reader:
            image_path = row["image_path"]
            question = row["question"]
            print(f"Processing: {image_path} -> {question}")

            audio_file = send_question_to_server(question)
            if audio_file:
                push_audio_to_android(audio_file)
                display_image_on_screen(image_path)
                trigger_gemini_api()
                trigger_adb_play_audio_broadcast()

                log_detected_event = threading.Event()
                is_question_audio_play_done(log_detected_event)
                log_detected_event.wait()

                trigger_adb_start_recording_broadcast()
                time.sleep(5)
                trigger_adb_stop_recording_broadcast()
                pull_audio_from_android()
                send_response_to_server(question, "static/gemini_output.mp3")


def send_question_to_server(question):
    try:
        response = requests.post(QUESTION_SERVER_URL, json={"question": question})
        if response.status_code == 200:
            audio_file_path = "static/output_audio.mp3"
            with open(audio_file_path, "wb") as file:
                file.write(response.content)
            return audio_file_path
        else:
            print("Error: Failed to get audio file")
            return None
    except Exception as e:
        print(f"Exception: {e}")
        return None


def display_image_on_screen(image_path):
    pass


def push_audio_to_android(audio_file):
    try:
        subprocess.run(["adb", "push", audio_file, AUDIO_PUSH_PATH], check=True)
        print("Audio file pushed successfully.")
    except subprocess.CalledProcessError as e:
        print(f"Error pushing audio file: {e}")


def trigger_gemini_api():
    print("Gemini started.")
    pass


def trigger_adb_play_audio_broadcast():
    try:
        subprocess.run(["adb", "shell"] + ADB_PLAY_AUDIO_BROADCAST.split(), check=True)
        print("ADB play recording broadcast triggered.")
    except subprocess.CalledProcessError as e:
        print(f"Error triggering ADB play recording broadcast: {e}")


def is_question_audio_play_done(log_detected_event):
    def logcat_listener():
        try:
            process = subprocess.Popen(["adb", "logcat", "-c"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            process = subprocess.Popen(["adb", "logcat", "-s", LOGCAT_FILTER], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            for line in iter(process.stdout.readline, b""):
                log_line = line.decode("utf-8").strip()
                if "AudioPlayer: Playback completed" in log_line:
                    print("AudioPlayer: Playback completed. Log detected !!!")
                    log_detected_event.set()
                    process.terminate()
                    return True
        except Exception as e:
            print(f"Error in logcat monitoring: {e}")

    threading.Thread(target=logcat_listener, daemon=True).start()


def trigger_adb_start_recording_broadcast():
    try:
        subprocess.run(["adb", "shell"] + ADB_START_RECORD_AUDIO_BROADCAST.split(), check=True)
        print("ADB start recording broadcast triggered.")
    except subprocess.CalledProcessError as e:
        print(f"Error triggering ADB start recording broadcast: {e}")


def trigger_adb_stop_recording_broadcast():
    try:
        subprocess.run(["adb", "shell"] + ADB_STOP_RECORT_AUDIO_BROADCAST.split(), check=True)
        print("ADB stop recording broadcast triggered.")
    except subprocess.CalledProcessError as e:
        print(f"Error triggering ADB stop recording broadcast: {e}")
        return


def pull_audio_from_android():
    try:
        subprocess.run(["adb", "pull", AUDIO_PULL_PATH, "static/gemini_output.mp3"], check=True)
        print("Audio file pulled successfully.")
    except subprocess.CalledProcessError as e:
        print(f"Error pulling audio file: {e}")


def send_response_to_server(question, audio_file_path):
    if not os.path.exists(audio_file_path):
        print("Error: Audio file not found")
        return False

    try:
        files = {"file": (os.path.basename(audio_file_path), open(audio_file_path, "rb"), "audio/mpeg")}
        data = {"question": question}
        response = requests.post(AUDIO_UPLOAD_SERVER_URL, files=files, data=data)

        if response.status_code == 200:
            print("File uploaded successfully.")
            return True
        else:
            print("Error: Failed to upload file")
            return False
    except Exception as e:
        print(f"Exception: {e}")
        return False


@app.route("/process", methods=["POST"])
def process_request():
    threading.Thread(target=process_csv, daemon=True).start()
    return jsonify({"message": "Processing started"}), 200


if __name__ == "__main__":
    app.run(port=5000, debug=True)
