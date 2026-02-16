<?php
require 'db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(['success' => false, 'error' => 'Only POST']);
    exit;
}

$login = isset($_POST['login']) ? trim($_POST['login']) : '';
$pass = isset($_POST['password']) ? trim($_POST['password']) : '';

if ($login === '' || $pass === '') {
    echo json_encode(['success' => false, 'error' => 'Missing credentials']);
    exit;
}

// Prepared statement to avoid injection
$stmt = mysqli_prepare($conn, "SELECT id, full_name, access_level FROM employee WHERE login = ? AND password = ? LIMIT 1");
mysqli_stmt_bind_param($stmt, 'ss', $login, $pass);
mysqli_stmt_execute($stmt);
$res = mysqli_stmt_get_result($stmt);
$user = mysqli_fetch_assoc($res);
mysqli_stmt_close($stmt);

if ($user) {
    echo json_encode(['success' => true, 'user' => $user]);
} else {
    echo json_encode(['success' => false, 'error' => 'Invalid credentials']);
}
?>
