# Real Time Edge Detection Viewer


Android + OpenCV + OpenGL pipeline that captures camera frames, processes them in native C++, and displays the result with OpenGL ES. Includes a companion TypeScript web viewer that showcases how processed frames can be surfaced to a light-weight web UI.

## Project Structure

```
.
├── app/                 # Android application module
├── gl/                  # Kotlin OpenGL renderer sources 
├── jni/                 # Native C++ source + CMake build
└── web/                 # TypeScript demo viewer
```

## Android Pipeline

1. `CameraController` streams YUV frames from Camera2 using a hidden `TextureView`.
2. `FrameProcessor` forwards frame buffers to `NativeProcessor` via JNI.
3. `Processor.cpp` runs OpenCV-based edge detection and returns an RGBA buffer.
4. `ProcessedFrameRenderer` uploads that buffer as a texture and renders full-screen with OpenGL ES 2.0.

### Requirements

- Android Studio Giraffe
- Android NDK with CMake
- OpenCV Android SDK with C++ headers and native libraries

### Run

1. Open the project in Android Studio.
2. Sync Gradle
3. NDK + CMake install when prompted.
4. Select a physical device (recommended) or an emulator with camera passthrough.
5. Build & run the `app` configuration.
