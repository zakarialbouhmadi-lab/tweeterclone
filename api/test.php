<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();
$response['success'] = true;
$response['message'] = "Connection successful";
$response['database'] = DB_NAME;
$response['host'] = DB_HOST;

echo json_encode($response);
?>
