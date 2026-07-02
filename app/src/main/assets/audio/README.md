# SpeakFluently Audio Assets Folder

You can upload your high-quality voice audio files (.wav or .mp3) directly here.
The app is configured to automatically detect files placed in these directories and play them offline with zero network download required!

## Directory Structure:

1. **Daily Conversational English**:
   - Save files in: `app/src/main/assets/audio/daily/`
   - Supported File Names:
     - `q_weekend_plans.wav` / `q_weekend_plans.mp3`
     - `q_favorite_hobby.wav` / `q_favorite_hobby.mp3`
     - `q_perfect_day.wav` / `q_perfect_day.mp3`
     - `q_weather_mood.wav` / `q_weather_mood.mp3`
     - `q_recommend_book.wav` / `q_recommend_book.mp3`

2. **IELTS Speaking Mastery**:
   - Save files in: `app/src/main/assets/audio/ielts/`
   - Supported File Names:
     - `q_ielts_hometown.wav` / `q_ielts_hometown.mp3`
     - `q_ielts_work_study.wav` / `q_ielts_work_study.mp3`
     - `q_ielts_technology.wav` / `q_ielts_technology.mp3`
     - `q_ielts_public_transport.wav` / `q_ielts_public_transport.mp3`
     - `q_ielts_future_plans.wav` / `q_ielts_future_plans.mp3`

3. **Job Interview Confidence**:
   - Save files in: `app/src/main/assets/audio/interview/`
   - Supported File Names:
     - `q_interview_introduce.wav` / `q_interview_introduce.mp3`
     - `q_interview_strength.wav` / `q_interview_strength.mp3`
     - `q_interview_conflict.wav` / `q_interview_conflict.mp3`
     - `q_interview_pressure.wav` / `q_interview_pressure.mp3`
     - `q_interview_failure.wav` / `q_interview_failure.mp3`

## How it works:
When the app starts, it checks if `audio/[category]/[file_name]` exists in your assets. 
If found, it plays it instantly from the local package. If not, it falls back to downloading or generating a synthetic voice!
