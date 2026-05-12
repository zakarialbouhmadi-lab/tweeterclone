<?php
header('Content-Type: application/json');
require_once 'config.php';
$response = array();

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $user_id = $_POST['user_id'];
    $username = $_POST['username'];
    $bio = $_POST['bio'];
    $is_public = isset($_POST['is_public']) ? (int)$_POST['is_public'] : 1;

    // Check if username is already taken by another user
    $check_stmt = $conn->prepare("SELECT user_id FROM users WHERE username = ? AND user_id != ?");
    $check_stmt->bind_param("si", $username, $user_id);
    $check_stmt->execute();

    if($check_stmt->get_result()->num_rows > 0) {
        $response['success'] = false;
        $response['message'] = "Username already taken";
        log_warn('update_profile', "Username taken - user_id:{$user_id} attempted_username:{$username}");
        echo json_encode($response);
        exit();
    }

    $stmt = $conn->prepare("UPDATE users SET username = ?, bio = ?, is_public = ? WHERE user_id = ?");
    $stmt->bind_param("ssii", $username, $bio, $is_public, $user_id);

    if ($stmt->execute()) {
        $response['success'] = true;
        $response['message'] = "Profile updated successfully";
        log_info('update_profile', "Profile updated - user_id:{$user_id} username:{$username} is_public:{$is_public}");
    } else {
        $response['success'] = false;
        $response['message'] = "Error updating profile";
        log_error('update_profile', "DB update failed - user_id:{$user_id}");
    }
} else {
    $response['success'] = false;
    $response['message'] = "Invalid request method";
    log_warn('update_profile', "Invalid request method: " . $_SERVER['REQUEST_METHOD']);
}

echo json_encode($response);
?>