#!/usr/bin/env python3
import sys
import os
from gtts import gTTS
import logging

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def generate_audio(text, output_path, audio_type):
    try:
        # Clean the text
        text = text.strip()
        if not text:
            logger.error("Empty text provided")
            return False

        # Create directory if it doesn't exist
        os.makedirs(os.path.dirname(output_path), exist_ok=True)

        # Configure TTS settings based on type
        if audio_type == "word":
            # For single words, use slower speech
            tts = gTTS(text=text, lang='en', slow=True)
        else:
            # For sentences, use normal speed
            tts = gTTS(text=text, lang='en', slow=False)

        # Save the audio file
        tts.save(output_path)

        # Verify file was created
        if os.path.exists(output_path) and os.path.getsize(output_path) > 0:
            logger.info(f"Successfully generated {audio_type} audio: {output_path}")
            return True
        else:
            logger.error(f"Failed to create audio file: {output_path}")
            return False

    except Exception as e:
        logger.error(f"Error generating TTS for '{text}': {str(e)}")
        return False

if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("Usage: python3 tts_generator.py <text> <output_path> <type>")
        sys.exit(1)

    text = sys.argv[1]
    output_path = sys.argv[2]
    audio_type = sys.argv[3]

    success = generate_audio(text, output_path, audio_type)
    sys.exit(0 if success else 1)
