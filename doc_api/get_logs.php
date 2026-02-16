<?php
require 'db.php';
$res = mysqli_query($conn, "SELECT id, document_id, user_id, action, DATE_FORMAT(timestamp, '%Y-%m-%d %H:%i:%s') AS timestamp FROM log_record ORDER BY timestamp DESC LIMIT 200");
$out = [];
while ($r = mysqli_fetch_assoc($res)) { $out[] = $r; }
echo json_encode($out);
?>
