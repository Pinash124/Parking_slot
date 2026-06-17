import "./css/Register.css";
import { useNavigate } from "react-router-dom";
function Register() {
    const Home = useNavigate();
  return (
    <div className="register-container">
      <div className="register-bg"></div>

      <div className="register-main">
        <div className="register-welcome">
          <h1 className="register-title">ParkingSmart</h1>
          <p className="register-text">
            Tạo tài khoản mới để bắt đầu sử dụng dịch vụ của chúng tôi.
          </p>
        </div>

        <div className="register-form-wrapper">
          <div className="register-form-box">
            <div className="register-form-header">
              <h3 className="register-form-title">Đăng ký tài khoản</h3>
              <p className="register-form-subtitle">
                Bạn đã có tài khoản?{" "}
                <a href="/Login" className="register-link">
                  Đăng nhập
                </a>
              </p>
            </div>

            <form className="register-form-body">
              <input
                type="text"
                name="username"
                placeholder="Tên đăng nhập"
                className="register-input"
              />
              <input
                type="text"
                name="fullName"
                placeholder="Họ và tên"
                className="register-input"
              />
              <input
                type="email"
                name="email"
                placeholder="Email"
                className="register-input"
              />
              <input
                type="password"
                name="password"
                placeholder="Mật khẩu"
                className="register-input"
              />
              <input
                type="password"
                name="confirmPassword"
                placeholder="Xác nhận mật khẩu"
                className="register-input"
              />
              <button type="submit" className="register-btn-primary">
                Đăng ký
              </button>
              <div className="register-divider">
                <span className="register-line"></span>
                <span className="register-or">Hoặc</span>
                <span className="register-line"></span>
              </div>

              <div className="register-social">
                <button className="register-btn-social register-google">
                  Google
                </button>
              </div>
            </form>
          </div>
        </div>
        <button className="register-btn-back" onClick={() => Home("/")}>
          Quay lại trang chủ
        </button>
      </div>
    </div>
  );
}

export default Register;
