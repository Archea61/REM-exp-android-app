<?php
require 'db.php';

// Только POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(['success' => false, 'error' => 'Only POST']);
    exit;
}

// Получаем входные данные (не путать с $_GET)
$reg = isset($_POST['reg_number']) ? trim($_POST['reg_number']) : null;
$type = isset($_POST['type_id']) ? intval($_POST['type_id']) : 0;
$title = isset($_POST['title']) ? trim($_POST['title']) : '';
$author = isset($_POST['author_id']) ? intval($_POST['author_id']) : 0;
$due = isset($_POST['due_date']) && $_POST['due_date'] !== '' ? trim($_POST['due_date']) : null;
$receiver = isset($_POST['receiver']) ? trim($_POST['receiver']) : null;
$status = isset($_POST['status']) ? trim($_POST['status']) : 'черновик';

// Базовая валидация
if ($title === '' || $author <= 0) {
    echo json_encode(['success' => false, 'error' => 'Missing required fields (title, author_id)']);
    exit;
}

// Экранируем все текстовые поля
$reg_esc = $reg !== null ? mysqli_real_escape_string($conn, $reg) : '';
$title_esc = mysqli_real_escape_string($conn, $title);
$receiver_esc = $receiver !== null ? mysqli_real_escape_string($conn, $receiver) : '';
$status_esc = mysqli_real_escape_string($conn, $status);
$due_esc = $due !== null && $due !== '' ? mysqli_real_escape_string($conn, $due) : null;

$type_int = intval($type);
$author_int = intval($author);

// Формируем запрос безопасно, подставляя NULL где нужно
$values = [];
$values[] = ($reg !== null && $reg !== '') ? "'{$reg_esc}'" : "NULL";
$values[] = "CURDATE()";
$values[] = $type_int > 0 ? $type_int : "NULL";
$values[] = "'{$title_esc}'";
$values[] = $author_int;
$values[] = ($receiver !== null && $receiver !== '') ? "'{$receiver_esc}'" : "NULL";
$values[] = ($status_esc !== '') ? "'{$status_esc}'" : "NULL";
$values[] = ($due_esc !== null && $due_esc !== '') ? "'{$due_esc}'" : "NULL";

$sql = "INSERT INTO document (reg_number, reg_date, type_id, title, author_id, receiver, status, due_date)
        VALUES (" . implode(", ", $values) . ")";

$res = mysqli_query($conn, $sql);

if ($res) {
    $insert_id = mysqli_insert_id($conn);
    // логируем без прерывания
    $log_sql = "INSERT INTO log_record (document_id, user_id, action) VALUES ($insert_id, $author_int, 'Создан документ')";
    @mysqli_query($conn, $log_sql);
    echo json_encode(['success' => true, 'id' => intval($insert_id)]);
} else {
    // В режиме разработки можно вернуть mysqli_error для отладки
    $err = mysqli_error($conn);
    echo json_encode(['success' => false, 'error' => 'DB error: ' . $err]);
}
?>
