<?php
require 'db.php';
$res = mysqli_query($conn, "CALL get_completed_docs()");
$out = fetch_all_assoc($res);
while (mysqli_more_results($conn) && mysqli_next_result($conn)) {;}
echo json_encode($out);
?>
