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
        echo json_encode($response);
        exit();
    }

    // Delete the comment
    $stmt = $conn->prepare("DELETE FROM comments WHERE comment_id = ? AND user_id = ?");
    $stmt->bind_param("ii", $comment_id, $user_id);

    if ($stmt->execute()) {
        $response['success'] = true;
        $response['message'] = "Comment deleted successfully";
    } else {
        $response['success'] = false;
        $response['message'] = "Error deleting comment";
    }
} else {
    $response['success'] = false;
    $response['message'] = "Invalid request method";
}

echo json_encode($response);
?>
