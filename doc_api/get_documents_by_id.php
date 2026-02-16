<?php
require 'db.php';
$id = isset($_GET['id']) ? intval($_GET['id']) : 0;
if ($id <= 0) { echo json_encode(['success'=>false,'error'=>'Missing id']); exit; }

$sql = "SELECT d.*, dt.name AS type_name, e.full_name AS author_name
        FROM document d
        LEFT JOIN doc_type dt ON d.type_id = dt.id
        LEFT JOIN employee e ON d.author_id = e.id
        WHERE d.id = $id LIMIT 1";
$res = mysqli_query($conn, $sql);
$row = mysqli_fetch_assoc($res);
echo json_encode($row ? $row : []);
?>
