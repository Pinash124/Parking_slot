import "./NonAuthHeader.css";
import { useNavigate } from "react-router-dom";
function Header() {
  const Login = useNavigate();
  return (
    <header className="non-auth-header">
      <nav className="navbar">
        <div className="logo">
          <span className="logo-icon">
            <img src="/logo.png" alt="Logo" />
          </span>
          <span className="logo-text">ParkingSmart</span>
        </div>
        <ul className="nav-links">
          <li>
            <a href="#Home" className="active">
              Trang chủ
            </a>
          </li>
          <li>
            <a href="#Reservation">Đặt chỗ</a>
          </li>
          <li>
            <a href="#News">Tin tức</a>
          </li>
          <li>
            <a href="#Support">Hỗ trợ</a>
          </li>
        </ul>
        <button className="btn-signin" onClick={() => Login("/Login")}>
          Đăng nhập
        </button>
      </nav>

      <div className="hero">
        <div className="hero-content">
          <div className="hero-text">
            <h1>
              Giải pháp thông minh
              <br />
              cho mọi nhu cầu đỗ xe
            </h1>
            <p>
              Trải nghiệm quản lý đậu xe dễ dàng với tính năng theo dõi vị trí
              đậu xe theo thời gian thực, thanh toán tự động và các tiện ích an
              toàn được thiết kế dành cho người lái xe hiện đại.
            </p>
            <div className="hero-buttons">
              <button className="btn-primary">Learn More</button>
            </div>
          </div>
          <div className="hero-image">
            <img
              src="https://images.unsplash.com/photo-1506521781263-d8422e82f27a?w=600&h=400&fit=crop"
              alt="Modern parking garage"
            />
          </div>
        </div>
      </div>
    </header>
  );
}

export default Header;
