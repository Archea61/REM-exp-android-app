<?php
require 'db.php';
$term = isset($_POST['term']) ? trim($_POST['term']) : '';
if ($term === '') { echo json_encode([]); exit; }

$term_esc = mysqli_real_escape_string($conn, $term);
$res = mysqli_query($conn, "CALL search_documents_by_title('{$term_esc}')");
$out = fetch_all_assoc($res);
while (mysqli_more_results($conn) && mysqli_next_result($conn)) {;}
echo json_encode($out);
?>
