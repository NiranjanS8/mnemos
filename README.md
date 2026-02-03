# Mnemos

A modern, feature-rich **personal productivity desktop application** built with JavaFX. Mnemos helps you manage tasks, take notes, organize files, and stay focused with an integrated Pomodoro timer.

![Java](https://img.shields.io/badge/Java-21-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.1-blue)
![SQLite](https://img.shields.io/badge/SQLite-Database-green)

---

## ✨ Features

### 📋 Task Management
- Create, edit, and organize tasks with priorities
- Set reminders and due dates
- Track task completion with streaks
- Automated task cleanup and scheduling

### 📝 Notes
- Rich note-taking capabilities
- Link notes to tasks and files for context

### 📁 File Management
- Organize and reference files within the app
- Link files to related tasks and notes

### ⏱️ Pomodoro Timer
- Built-in Pomodoro technique timer
- Customizable work and break durations
- Stay focused and productive

### 🔗 Linking System
- Create links between tasks, notes, and files
- Build a connected knowledge graph of your work

### 🔐 Security
- Password protection for your data
- Secure authentication system

### 🖥️ Desktop Integration
- System tray support for quick access
- Window state persistence
- Command palette for quick navigation (keyboard shortcuts)

---

## 🛠️ Tech Stack

| Technology | Purpose |
|------------|---------|
| **Java 21** | Core language |
| **JavaFX 21** | UI framework |
| **SQLite** | Local database storage |
| **Maven** | Build and dependency management |

---

## 📦 Installation

### Prerequisites
- **Java 21** or higher installed
- **Maven 3.6+** installed

### Build from Source

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd mnemos
   ```

2. **Build the project**
   ```bash
   mvn clean package
   ```

3. **Run the application**
   ```bash
   mvn javafx:run
   ```

### Create Executable

To create a native Windows installer/executable:

```bash
mvn clean package jpackage:jpackage
```

The installer will be generated in `target/installer/`.

---

## 🚀 Usage

### Running the App
```bash
mvn javafx:run
```

### First-Time Setup
1. Launch the application
2. Set up your password for data protection
3. Start creating tasks, notes, and organizing your work!

### Keyboard Shortcuts
- **Command Palette**: Quick access to all app commands and navigation

---

## 📁 Project Structure

```
src/main/java/com/mnemos/
├── App.java                 # Main application entry point
├── command/                 # Command pattern implementations
├── model/                   # Data models (Task, Note, Link, etc.)
├── repository/              # Data access layer
├── service/                 # Business logic services
├── ui/                      # JavaFX controllers
└── util/                    # Utility classes (DatabaseManager, etc.)

src/main/resources/com/mnemos/ui/
├── *.fxml                   # JavaFX view definitions
└── styles.css               # Application styling
```

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

---

## 📄 License

This project is open source. See the LICENSE file for details.

---

## 👤 Author

**Mnemos Team**

---

*Built with ❤️ using JavaFX*
