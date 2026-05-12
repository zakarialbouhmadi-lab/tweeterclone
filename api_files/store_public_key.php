<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(['success' => false, 'message' => 'Invalid request method']);
    exit();
}

$user_id   = isset($_POST['user_id'])   ? (int)$_POST['user_id']        : 0;
$public_key = isset($_POST['public_key']) ? trim($_POST['public_key'])   : '';

if ($user_id === 0 || empty($public_key)) {
    echo json_encode(['success' => false, 'message' => 'Invalid parameters']);
    exit();
}

try {
    $stmt = $conn->prepare("
        INSERT INTO user_public_keys (user_id, public_key)
        VALUES (?, ?)
        ON DUPLICATE KEY UPDATE public_key = VALUES(public_key), updated_at = CURRENT_TIMESTAMP
    ");
    $stmt->bind_param("is", $user_id, $public_key);
    $stmt->execute();

    log_info('store_public_key', "Public key stored - user_id:{$user_id}");
    echo json_encode(['success' => true, 'message' => 'Public key stored']);
} catch (Exception $e) {
    log_error('store_public_key', "Failed - user_id:{$user_id} msg:" . $e->getMessage());
    echo json_encode(['success' => false, 'message' => $e->getMessage()]);
}
?>
