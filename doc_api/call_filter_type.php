<?php
require 'db.php';
$type = isset($_POST['type']) ? trim($_POST['type']) : '';
if ($type === '') { echo json_encode([]); exit; }

$type_esc = mysqli_real_escape_string($conn, $type);
$q = "CALL filter_by_type('{$type_esc}')";
$res = mysqli_query($conn, $q);
$out = fetch_all_assoc($res);

// очистка возможных дополнительных resultsets
while (mysqli_more_results($conn) && mysqli_next_result($conn)) {;}

echo json_encode($out);
?>
