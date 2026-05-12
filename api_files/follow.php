<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $follower_id = (int)$_POST['follower_id'];
    $following_id = (int)$_POST['following_id'];

    // Check if there's already a follow entry (any status)
    $check_stmt = $conn->prepare("SELECT status FROM follows WHERE follower_id = ? AND following_id = ?");
    $check_stmt->bind_param("ii", $follower_id, $following_id);
    $check_stmt->execute();
    $result = $check_stmt->get_result();

    if ($result->num_rows > 0) {
        $row = $result->fetch_assoc();
        $current_status = $row['status'];
        
        if ($current_status == 'accepted') {
            // Already following, so unfollow
            $stmt = $conn->prepare("DELETE FROM follows WHERE follower_id = ? AND following_id = ?");
            $stmt->bind_param("ii", $follower_id, $following_id);
            $follow_status = 'none';
            $message = "Successfully unfollowed";
        } else if ($current_status == 'pending') {
            // Cancel pending request
            $stmt = $conn->prepare("DELETE FROM follows WHERE follower_id = ? AND following_id = ?");
            $stmt->bind_param("ii", $follower_id, $following_id);
            $follow_status = 'none';
            $message = "Follow request cancelled";
        } else {
            // Status is 'declined', create new pending request
            $stmt = $conn->prepare("UPDATE follows SET status = 'pending', created_at = CURRENT_TIMESTAMP WHERE follower_id = ? AND following_id = ?");
            $stmt->bind_param("ii", $follower_id, $following_id);
            $follow_status = 'pending';
            $message = "Follow request sent";
        }
    } else {
        // No existing follow entry, create new pending request
        $stmt = $conn->prepare("INSERT INTO follows (follower_id, following_id, status) VALUES (?, ?, 'pending')");
        $stmt->bind_param("ii", $follower_id, $following_id);
        $follow_status = 'pending';
        $message = "Follow request sent";
    }

    if ($stmt->execute()) {
        $count_stmt = $conn->prepare("SELECT COUNT(*) as count FROM follows WHERE following_id = ? AND status = 'accepted'");
        $count_stmt->bind_param("i", $following_id);
        $count_stmt->execute();
        $count_row = $count_stmt->get_result()->fetch_assoc();

        $response['success'] = true;
        $response['follow_status'] = $follow_status;
        $response['is_following'] = ($follow_status == 'accepted');
        $response['followers_count'] = $count_row['count'];
        $response['message'] = $message;
        log_info('follow', "{$message} - follower_id:{$follower_id} following_id:{$following_id} status:{$follow_status}");
    } else {
        $response['success'] = false;
        $response['message'] = "Error updating follow status";
        log_error('follow', "DB error - follower_id:{$follower_id} following_id:{$following_id}");
    }
} else {
    $response['success'] = false;
    $response['message'] = "Invalid request method";
    log_warn('follow', "Invalid request method: " . $_SERVER['REQUEST_METHOD']);
}

echo json_encode($response);
?>
