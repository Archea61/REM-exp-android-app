<?php
// db.php
header('Content-Type: application/json; charset=utf-8');

// Настройки БД — укажи свои значения
define('DB_HOST', '127.0.0.1');
define('DB_USER', 'root');
define('DB_PASS', '');
define('DB_NAME', 'doc_registry');
define('DB_PORT', '3307');

$conn = mysqli_connect(DB_HOST, DB_USER, DB_PASS, DB_NAME, DB_PORT);
if (!$conn) {
    http_response_code(500);
    echo json_encode(['error' => 'DB connect failed', 'errno' => mysqli_connect_errno(), 'error_str' => mysqli_connect_error()]);
    exit;
}
mysqli_set_charset($conn, "utf8mb4");

// Разрешаем CORS для разработки (если нужно, ограничь домен)
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

// helper: fetch all rows from mysqli_result into array
function fetch_all_assoc($res) {
    $rows = [];
    if ($res) {
        while ($r = mysqli_fetch_assoc($res)) {
            $rows[] = $r;
        }
    }
    return $rows;
}
?>
