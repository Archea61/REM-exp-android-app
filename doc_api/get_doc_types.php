<?php
require 'db.php';
$res = mysqli_query($conn, "SELECT id, name, description FROM doc_type ORDER BY name");
echo json_encode(fetch_all_assoc($res));
?>
