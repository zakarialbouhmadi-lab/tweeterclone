<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $tweet_id = $_POST['tweet_id'];
    $user_id = $_POST['user_id'];
    $content = $_POST['content'];

    $stmt = $conn->prepare("INSERT INTO comments (tweet_id, user_id, content) VALUES (?, ?, ?)");
    $stmt->bind_param("iis", $tweet_id, $user_id, $content);
    
    if ($stmt->execute()) {
        $response['success'] = true;
        $response['message'] = "Comment posted successfully";
    } else {
        $response['success'] = false;
        $response['message'] = "Error posting comment";
    }
} else {
    $response['success'] = false;
    $response['message'] = "Invalid request method";
}

echo json_encode($response);
?>
