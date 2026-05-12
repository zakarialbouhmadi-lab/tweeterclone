<?php
// No whitespace before opening PHP tag
require_once __DIR__ . '/logger.php';

define('DB_HOST', 'sql193.lh.pl');
define('DB_NAME', 'serwer440925_blog');
define('DB_USER', 'serwer440925_blog');
define('DB_PASS', 'B3rgsportsmierdz!');

$conn = new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME);
$conn->set_charset('utf8mb4');

if ($conn->connect_error) {
    log_error('config', 'DB connection failed: ' . $conn->connect_error);
    die(json_encode(array(
        'success' => false,
        'message' => 'Connection failed: ' . $conn->connect_error
    )));
}

$conn->set_charset("utf8");
// No extra output or echo statements
?>
