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
        echo json_encode($response);
        exit();
    }

    $stmt = $conn->prepare("UPDATE users SET username = ?, bio = ?, is_public = ? WHERE user_id = ?");
    $stmt->bind_param("ssii", $username, $bio, $is_public, $user_id);

    if ($stmt->execute()) {
        $response['success'] = true;
        $response['message'] = "Profile updated successfully";
    } else {
        $response['success'] = false;
        $response['message'] = "Error updating profile";
    }
} else {
    $response['success'] = false;
    $response['message'] = "Invalid request method";
}

echo json_encode($response);
?>