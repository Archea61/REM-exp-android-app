<?php
require 'db.php';
if ($_SERVER['REQUEST_METHOD'] !== 'POST') { echo json_encode(['success'=>false,'error'=>'Only POST']); exit; }

$id = isset($_POST['id']) ? intval($_POST['id']) : 0;
if ($id <= 0) { echo json_encode(['success'=>false,'error'=>'Missing id']); exit; }

$fields = [];
$params = [];
$types = '';

if (isset($_POST['title'])) { $fields[]='title=?'; $params[] = $_POST['title']; $types .= 's'; }
if (isset($_POST['type_id'])) { $fields[]='type_id=?'; $params[] = intval($_POST['type_id']); $types .= 'i'; }
if (isset($_POST['status'])) { $fields[]='status=?'; $params[] = $_POST['status']; $types .= 's'; }
if (isset($_POST['due_date'])) { $fields[]='due_date=?'; $params[] = $_POST['due_date']; $types .= 's'; }
if (isset($_POST['receiver'])) { $fields[]='receiver=?'; $params[] = $_POST['receiver']; $types .= 's'; }

if (count($fields) === 0) { echo json_encode(['success'=>false,'error'=>'No fields to update']); exit; }

$sql = "UPDATE document SET " . implode(', ', $fields) . " WHERE id = ?";
$params[] = $id;
$types .= 'i';

$stmt = mysqli_prepare($conn, $sql);
mysqli_stmt_bind_param($stmt, $types, ...$params);
$ok = mysqli_stmt_execute($stmt);
$affected = mysqli_stmt_affected_rows($stmt);
mysqli_stmt_close($stmt);

// log action - we don't know user id here; optional
mysqli_query($conn, "INSERT INTO log_record (document_id, action) VALUES ($id, 'Обновлены поля документа')");

if ($ok) echo json_encode(['success'=>true,'affected'=>$affected]); else echo json_encode(['success'=>false,'error'=>mysqli_error($conn)]);
?>
