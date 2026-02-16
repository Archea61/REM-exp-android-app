Document Management System (Android + PHP + MySQL)
This project is a client–server Document Management System built as a practical implementation of a digital document registration workflow. The main goal was to replace manual document tracking with a structured, centralized system that ensures data integrity, transparency, and faster processing.
The system consists of an Android application written in Kotlin and a backend developed in PHP with a MySQL database. Communication between the mobile client and the server is performed via HTTP requests with JSON responses.
About the Project
In many organizations, document tracking is still handled manually or through poorly structured spreadsheets. This leads to duplicated records, missing information, and limited control over document execution.
This project solves those issues by providing:
centralized storage of documents,
structured relationships between departments, employees, and document types,
support for assigning executors,
full logging of document-related actions.
The application allows users to create, edit, delete, and view documents in a clean and straightforward mobile interface.
Architecture
The system follows a classic client–server architecture:
Android application → HTTP request → PHP backend → MySQL database → JSON response → UI update
The backend handles validation, database operations, and error processing. The Android client is responsible for user interaction and displaying data received from the server.
Foreign key constraints are used in the database to maintain referential integrity. All important operations related to documents are recorded in a dedicated log table.
Technologies Used
Backend:
PHP
MySQL
XAMPP (Apache + MySQL)
Mobile Application:
Kotlin
Android Studio
XML layouts
The project also follows REST-style principles for structuring endpoints and working with resources.
Functionality
The application supports full CRUD operations for documents. Each document can be linked to a department, assigned a type, and connected to one or multiple executors. The system also keeps a history of actions performed on documents.
Special attention was paid to:
correct JSON parsing,
network error handling,
validation on the server side,
proper handling of foreign key constraints.
