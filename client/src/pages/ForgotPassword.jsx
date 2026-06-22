import { useNavigate } from "react-router-dom";
import "./css/ForgotPassword.css";
function ForgotPassword() {
  const Home = useNavigate();
  return (
    <div className="forgot-container">
      <div className="forgot-bg"></div>

      <div className="forgot-main">
        <div className="forgot-welcome">
          <h1 className="forgot-title">ParkingSmart</h1>
          <p className="forgot-text">
            Nhập email của bạn để nhận liên kết đặt lại mật khẩu.
          </p>
        </div>

        <div className="forgot-form-wrapper">
          <div className="forgot-form-box">
            <div className="forgot-form-header">
              <h3 className="forgot-form-title">Quên mật khẩu</h3>
              <p className="forgot-form-subtitle">
                Bạn nhớ mật khẩu?{" "}
                <a href="/login" className="forgot-link">
                  Đăng nhập
                </a>
              </p>
            </div>

            <form className="forgot-form-body">
              <input
                type="email"
                name="email"
                placeholder="Email"
                className="forgot-input"
              />
              <button type="submit" className="forgot-btn-primary">
                Gửi yêu cầu
              </button>
            </form>
          </div>
        </div>
        <button className="forgot-btn-back" onClick={() => Home("/")}>
          Quay lại trang chủ
        </button>
      </div>
    </div>
  );
}

export default ForgotPassword;
