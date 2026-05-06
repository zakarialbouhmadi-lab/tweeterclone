<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $email = $_POST['email'];
    $password = $_POST['password'];

    $stmt = $conn->prepare("SELECT user_id, username, password FROM users WHERE email = ?");
    $stmt->bind_param("s", $email);
    $stmt->execute();
    $result = $stmt->get_result();

    if ($result->num_rows == 1) {
        $user = $result->fetch_assoc();
        if (password_verify($password, $user['password'])) {
            $response['success'] = true;
            $response['user_id'] = $user['user_id'];
            $response['username'] = $user['username'];  // Added username to response
            $response['message'] = "Login successful";
        } else {
            $response['success'] = false;
            $response['message'] = "Invalid password";
        }
    } else {
        $response['success'] = false;
        $response['message'] = "User not found";
    }
} else {
    $response['success'] = false;
    $response['message'] = "Invalid request method";
}

echo json_encode($response);
?>
