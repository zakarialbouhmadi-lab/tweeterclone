<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();
$tweet_id = isset($_GET['tweet_id']) ? $_GET['tweet_id'] : 0;

try {
    $query = "
    SELECT
    c.comment_id,
    c.user_id,
    u.username,
    c.content,
    c.created_at
    FROM comments c
    JOIN users u ON c.user_id = u.user_id
    WHERE c.tweet_id = ?
    ORDER BY c.created_at DESC";

    $stmt = $conn->prepare($query);
    $stmt->bind_param("i", $tweet_id);
    $stmt->execute();
    $result = $stmt->get_result();

    $comments = array();
    while ($row = $result->fetch_assoc()) {
        $comments[] = $row;
    }

    $response['success'] = true;
    $response['comments'] = $comments;

} catch (Exception $e) {
    log_error('get_comments', "Error - tweet_id:{$tweet_id} msg:" . $e->getMessage());
    $response['success'] = false;
    $response['message'] = "Error: " . $e->getMessage();
}

echo json_encode($response);
?>
