#!/usr/bin/env python3
import sys
import os
import subprocess
import tempfile
from gtts import gTTS
import logging

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def check_ffmpeg():
    """Check if FFmpeg is available"""
    try:
        subprocess.run(
            ['ffmpeg', '-version'],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=True
        )
        return True
    except (subprocess.CalledProcessError, FileNotFoundError):
        logger.warning("FFmpeg not found. Speed adjustment will be skipped.")
        return False

def adjust_speed_with_ffmpeg(input_file, output_file, speed_factor):
    """Adjust audio speed using FFmpeg atempo filter"""
    try:
        if speed_factor == 1.0:
            subprocess.run(['cp', input_file, output_file], check=True)
            return True

        cmd = [
            'ffmpeg', '-i', input_file,
            '-filter:a', "atempo={}".format(speed_factor),
            '-y', output_file
        ]
        result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True)

        if result.returncode == 0:
            logger.info("Successfully adjusted audio speed by {}x".format(speed_factor))
            return True
        else:
            logger.error("FFmpeg error: {}".format(result.stderr))
            return False

    except Exception as e:
        logger.error("Error adjusting speed with FFmpeg: {}".format(str(e)))
        return False

def generate_audio(text, output_path, audio_type, speed_factor=1.2):
    """Generate TTS audio and adjust speed if requested"""
    try:
        text = text.strip()
        if not text:
            logger.error("Empty text provided")
            return False

        # Create directory if it doesn't exist (Python 3.6 compatible)
        output_dir = os.path.dirname(output_path)
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)

        # Configure TTS settings
        if audio_type == "word":
            tts = gTTS(text=text, lang='en', slow=True)
        else:
            tts = gTTS(text=text, lang='en', slow=False)

        ffmpeg_available = check_ffmpeg()

        if ffmpeg_available and speed_factor != 1.0:
            with tempfile.NamedTemporaryFile(suffix='.mp3', delete=False) as temp_file:
                temp_path = temp_file.name

            # Generate base audio
            tts.save(temp_path)

            # Adjust speed
            success = adjust_speed_with_ffmpeg(temp_path, output_path, speed_factor)

            # Cleanup
            try:
                os.unlink(temp_path)
            except:
                pass

            if not success:
                logger.warning("Speed adjustment failed, using original audio")
                tts.save(output_path)
        else:
            tts.save(output_path)

        if os.path.exists(output_path) and os.path.getsize(output_path) > 0:
            speed_info = " (speed: {}x)".format(speed_factor) if speed_factor != 1.0 else ""
            logger.info("Successfully generated {} audio: {}{}".format(audio_type, output_path, speed_info))
            return True
        else:
            logger.error("Failed to create audio file: {}".format(output_path))
            return False

    except Exception as e:
        logger.error("Error generating TTS for '{}': {}".format(text, str(e)))
        return False

if __name__ == "__main__":
    if len(sys.argv) < 4:
        print("Usage: python3 tts_generator.py <text> <output_path> <type> [speed_factor]")
        sys.exit(1)

    text = sys.argv[1]
    output_path = sys.argv[2]
    audio_type = sys.argv[3]
    speed_factor = float(sys.argv[4]) if len(sys.argv) > 4 else 1.2

    success = generate_audio(text, output_path, audio_type, speed_factor)
    sys.exit(0 if success else 1)
