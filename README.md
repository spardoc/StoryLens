# 👧📚 StoryLens

**Version:** 1.0

**Short Description**
**StoryLens** is an Android application designed to **encourage creative writing in children**. It helps young writers build their own stories, generate colorful comic-style illustrations using artificial intelligence, and even capture handwritten or printed text with the camera to include in their digital storybooks.

---

## 🌟 Why StoryLens?

* **Motivates young writers**: Children choose characters, settings, and plot twists to build their own unique stories.
* **Visual and interactive learning**: AI-generated illustrations bring their words to life, supporting imagination and comprehension.
* **Active participation**: Kids can take pictures of their own handwriting or text and place them directly into their comic panels.

---

## ✨ Key Features

### ✍️ Guided Story Creation

* Step-by-step story builder with multiple choices for characters, settings, and actions.
* Live preview of the story as the child makes choices.
* Includes 8 narrative steps from introduction to resolution, helping children understand story structure.

### 🎨 AI-Generated Illustrations

* Uses **DALL·E 3** to create vivid, comic-style images from story descriptions.
* Comic book aesthetic with bold lines and dynamic compositions.
* Generates one image at a time for a smoother, interactive experience.

### 📸 Text Capture via Camera

* Kids can snap pictures of handwritten notes or printed sentences.
* Image processing pipeline powered by **OpenCV**:

  * Contrast enhancement (CLAHE)
  * Edge sharpening (unsharp mask)
  * Smoothing (bilateral filter)
* Add playful text bubble shapes like speech balloons, clouds, and rounded boxes.

### 🖼️ Story Composition Interface

* Drag and scale interface to position text bubbles over generated images.
* Accurate coordinate mapping for correct placement.
* Real-time preview of layout changes.

### 📄 Save & Share Stories

* Export full stories as **PDFs**.
* Save and manage stories in the **child’s personal library**.
* **Firebase** cloud storage allows safe and easy access to saved stories.

---

## 🛠️ Technical Overview

### Core Technologies

* **Android SDK** (using CameraX for capturing images)
* **OpenCV** (C++ based image enhancement)
* **Firebase** (Authentication, Firestore Database, Cloud Storage)
* **OpenAI API** for image generation with DALL·E 3
* **Retrofit** for API calls

### Key Components

* `MainActivity`: Interface for story building
* `ImageGenerator`: Manages AI-generated illustrations
* `CameraActivity`: Captures and processes text images
* `ComposeActivity`: Final story page editor and composition tool

---

## 📱 Requirements

### Device Requirements

* **Android 7.0 (API level 24)** or higher
* **Camera access** for text capture
* **Internet connection** for AI-based image generation

### Permissions

* `CAMERA`: Capture handwritten/printed text
* `INTERNET`: Access to OpenAI API
* `ACCESS_NETWORK_STATE`: Check network connectivity
* `READ_EXTERNAL_STORAGE`: Process saved image files

---

## ⚙️ Installation & Setup

1. Add your **OpenAI API key** to the project configuration.
2. Set up a **Firebase project**, including Authentication, Firestore, and Cloud Storage.
3. Download and link **OpenCV native libraries** with NDK support.
4. Open the project in **Android Studio** (latest version recommended).
5. Build and run the app on an Android device.

---

## 🚀 How It Works

1. **Create a Story**: The child selects options for each part of the narrative.
2. **Generate Art**: AI illustrations are generated based on story events.
3. **Capture Text** (Optional): Use the camera to include handwritten or book text.
4. **Add Text Bubbles**: Choose a fun shape and attach it to the image.
5. **Arrange Elements**: Drag and position bubbles on the scene.
6. **Export to PDF**: Save the comic and add it to the child’s digital story library.

---

## 🧠 Educational Benefits

* Promotes **creative writing** and **story structure understanding**.
* Encourages **reading and visual learning** through image and text integration.
* Builds confidence by allowing children to **author and illustrate** their own books.

---

**StoryLens** turns imagination into illustrated stories — helping kids become authors, artists, and creators of their own worlds. 🧒✨📖

---
