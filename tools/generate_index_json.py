#!/usr/bin/env python3
"""
generate_index_json.py
Generates index.json for Speak-Fluently app from a local directory of audio files.

Usage:
  python generate_index_json.py <audio_folder> <base_url> [output_file]

Example:
  python generate_index_json.py C:\audio-files https://mysite.com/audio

This scans all subfolders in <audio_folder>, finds audio files (wav/mp3/m4a/ogg/flac),
and creates index.json with the correct download URLs.

Upload index.json + audio files to your host and update HOST_BASE_URL in SyncConfig.kt.
"""

import os
import sys
import json
import re
from pathlib import Path

AUDIO_EXTENSIONS = {".wav", ".mp3", ".m4a", ".ogg", ".flac", ".aac", ".wma"}

def extract_number(filename: str) -> int:
    """Extract first number from filename. e.g. 'speech-1.wav' -> 1, '03_hello.mp3' -> 3"""
    name = Path(filename).stem
    numbers = re.findall(r'\d+', name)
    return int(numbers[0]) if numbers else 0

def clean_title(filename: str) -> str:
    """Make a human-readable title from filename. e.g. 'speech-3.wav' -> 'سوال ۳'"""
    name = Path(filename).stem
    # Try to find a number
    numbers = re.findall(r'\d+', name)
    if numbers:
        return f"سوال {numbers[0]}"
    # Fallback: remove underscores/hyphens
    return name.replace("_", " ").replace("-", " ").strip()

def generate_index(audio_folder: str, base_url: str) -> dict:
    audio_dir = Path(audio_folder)
    if not audio_dir.exists():
        print(f"Error: Directory not found: {audio_folder}")
        sys.exit(1)

    # Strip trailing slash from base URL
    base_url = base_url.rstrip("/")
    
    exercises = []
    
    # Find all subfolders (sorted by number)
    subfolders = sorted(
        [d for d in audio_dir.iterdir() if d.is_dir()],
        key=lambda d: int(re.findall(r'\d+', d.name)[0]) if re.findall(r'\d+', d.name) else 9999
    )
    
    for folder in subfolders:
        # Find all audio files in this folder
        audio_files = sorted(
            [f for f in folder.iterdir() if f.suffix.lower() in AUDIO_EXTENSIONS and f.is_file()],
            key=lambda f: extract_number(f.name)
        )
        
        if not audio_files:
            continue
        
        # Extract exercise ID from folder name
        folder_numbers = re.findall(r'\d+', folder.name)
        exercise_id = int(folder_numbers[0]) if folder_numbers else len(exercises) + 1
        
        files = []
        for idx, audio_file in enumerate(audio_files, 1):
            # Build URL: base_url/folder_name/filename
            url = f"{base_url}/{folder.name}/{audio_file.name}"
            title = clean_title(audio_file.name)
            
            files.append({
                "id": idx,
                "title": title,
                "url": url
            })
        
        exercises.append({
            "id": exercise_id,
            "name": f"تمرین روز {exercise_id}",
            "files": files
        })
        
        print(f"  Exercise {exercise_id}: {len(files)} files from '{folder.name}/'")
    
    return {"exercises": exercises}

def main():
    if len(sys.argv) < 3:
        print(__doc__)
        print("\nArguments:")
        print("  audio_folder  - Path to folder containing exercise subfolders")
        print("  base_url      - Base URL where files will be hosted")
        print("  output_file   - Output path for index.json (default: ./index.json)")
        print("\nExample:")
        print('  python generate_index_json.py C:\\my-audio https://mysite.com/audio')
        sys.exit(1)
    
    audio_folder = sys.argv[1]
    base_url = sys.argv[2]
    output_file = sys.argv[3] if len(sys.argv) > 3 else "index.json"
    
    print(f"Scanning: {audio_folder}")
    print(f"Base URL: {base_url}")
    print()
    
    index = generate_index(audio_folder, base_url)
    
    # Write JSON
    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(index, f, ensure_ascii=False, indent=2)
    
    total_exercises = len(index["exercises"])
    total_files = sum(len(ex["files"]) for ex in index["exercises"])
    
    print(f"\nDone!")
    print(f"  Output: {output_file}")
    print(f"  Exercises: {total_exercises}")
    print(f"  Total files: {total_files}")
    print(f"\nNext steps:")
    print(f"  1. Upload {output_file} and all audio files to your host")
    print(f"  2. Update HOST_BASE_URL in SyncConfig.kt to '{base_url}'")
    print(f"  3. Build and install the app")

if __name__ == "__main__":
    main()