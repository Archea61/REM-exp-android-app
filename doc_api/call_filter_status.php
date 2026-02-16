<?php
require 'db.php';
$status = isset($_POST['status']) ? trim($_POST['status']) : '';
if ($status === '') { echo json_encode([]); exit; }

$status_esc = mysqli_real_escape_string($conn, $status);
$q = "CALL filter_by_status('{$status_esc}')";
$res = mysqli_query($conn, $q);
$out = fetch_all_assoc($res);
while (mysqli_more_results($conn) && mysqli_next_result($conn)) {;}
echo json_encode($out);
?>
