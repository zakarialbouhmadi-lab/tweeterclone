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
        $comment_id = $conn->insert_id;
        $response['success'] = true;
        $response['message'] = "Comment posted successfully";
        log_info('post_comment', "Comment posted - user_id:{$user_id} tweet_id:{$tweet_id} comment_id:{$comment_id}");
    } else {
        $response['success'] = false;
        $response['message'] = "Error posting comment";
        log_error('post_comment', "DB insert failed - user_id:{$user_id} tweet_id:{$tweet_id}");
    }
} else {
    $response['success'] = false;
    $response['message'] = "Invalid request method";
    log_warn('post_comment', "Invalid request method: " . $_SERVER['REQUEST_METHOD']);
}

echo json_encode($response);
?>
