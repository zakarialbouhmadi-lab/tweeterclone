<?php
header('Content-Type: application/json');
require_once 'config.php';
$response = array();

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $comment_id = $_POST['comment_id'];
    $user_id = $_POST['user_id'];

    // Verify the comment belongs to the user
    $check_stmt = $conn->prepare("SELECT user_id FROM comments WHERE comment_id = ?");
    $check_stmt->bind_param("i", $comment_id);
    $check_stmt->execute();
    $result = $check_stmt->get_result();

    if($result->fetch_assoc()['user_id'] != $user_id) {
        $response['success'] = false;
        $response['message'] = "Unauthorized to delete this comment";
        log_warn('delete_comment', "Unauthorized delete attempt - user_id:{$user_id} comment_id:{$comment_id}");
        echo json_encode($response);
        exit();
    }

    // Delete the comment
    $stmt = $conn->prepare("DELETE FROM comments WHERE comment_id = ? AND user_id = ?");
    $stmt->bind_param("ii", $comment_id, $user_id);

    if ($stmt->execute()) {
        $response['success'] = true;
        $response['message'] = "Comment deleted successfully";
        log_info('delete_comment', "Comment deleted - user_id:{$user_id} comment_id:{$comment_id}");
    } else {
        $response['success'] = false;
        $response['message'] = "Error deleting comment";
        log_error('delete_comment', "DB delete failed - user_id:{$user_id} comment_id:{$comment_id}");
    }
} else {
    $response['success'] = false;
    $response['message'] = "Invalid request method";
    log_warn('delete_comment', "Invalid request method: " . $_SERVER['REQUEST_METHOD']);
}

echo json_encode($response);
?>
