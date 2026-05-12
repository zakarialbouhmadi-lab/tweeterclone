<?php
header('Content-Type: application/json');
require_once 'config.php';

$user_id = isset($_GET['user_id']) ? (int)$_GET['user_id'] : 0;

if ($user_id === 0) {
    echo json_encode(['success' => false, 'message' => 'Invalid user ID']);
    exit();
}

try {
    $stmt = $conn->prepare("SELECT public_key FROM user_public_keys WHERE user_id = ?");
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();

    if ($result->num_rows === 0) {
        log_warn('get_public_key', "Key not found - user_id:{$user_id}");
        echo json_encode(['success' => false, 'message' => 'Public key not found']);
    } else {
        $row = $result->fetch_assoc();
        echo json_encode(['success' => true, 'public_key' => $row['public_key']]);
    }
} catch (Exception $e) {
    log_error('get_public_key', "Error - user_id:{$user_id} msg:" . $e->getMessage());
    echo json_encode(['success' => false, 'message' => $e->getMessage()]);
}
?>
