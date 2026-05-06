<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $content = $_POST['content'];
    $user_id = $_POST['user_id'];
    $image = isset($_POST['image']) ? $_POST['image'] : null;

    $stmt = $conn->prepare("INSERT INTO tweets (user_id, content, image) VALUES (?, ?, ?)");
    $stmt->bind_param("iss", $user_id, $content, $image);

    if ($stmt->execute()) {
        $response['success'] = true;
        $response['message'] = "Tweet created successfully";
        $response['tweet_id'] = $conn->insert_id;
    } else {
        $response['success'] = false;
        $response['message'] = "Error creating tweet";
    }
} else {
    $response['success'] = false;
    $response['message'] = "Invalid request method";
}

echo json_encode($response);
?>