<?php
require 'db.php';
if ($_SERVER['REQUEST_METHOD'] !== 'POST') { echo json_encode(['success'=>false,'error'=>'Only POST']); exit; }

$doc = isset($_POST['document_id']) ? intval($_POST['document_id']) : 0;
$emp = isset($_POST['employee_id']) ? intval($_POST['employee_id']) : 0;
$status = isset($_POST['exec_status']) ? trim($_POST['exec_status']) : 'назначен';

if ($doc <= 0 || $emp <= 0) { echo json_encode(['success'=>false,'error'=>'Missing ids']); exit; }

$stmt = mysqli_prepare($conn, "INSERT INTO doc_executor (document_id, employee_id, exec_status) VALUES (?, ?, ?)");
mysqli_stmt_bind_param($stmt, 'iis', $doc, $emp, $status);
$ok = mysqli_stmt_execute($stmt);
mysqli_stmt_close($stmt);

if ($ok) {
    mysqli_query($conn, "INSERT INTO log_record (document_id, user_id, action) VALUES ($doc, $emp, 'Назначен исполнитель')");
    echo json_encode(['success'=>true]);
} else {
    echo json_encode(['success'=>false,'error'=>mysqli_error($conn)]);
}
?>
