import { useNavigate } from 'react-router-dom';

export default function Placeholder({ title }) {
    const navigate = useNavigate();

    return (
        <div className="page-content">
            <div className="page-header">
                <button className="back-btn" onClick={() => navigate(-1)}>
                    ←
                </button>
                <h1 className="page-title">{title}</h1>
            </div>

            <div className="content-block">
                <p style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '40px 0', margin: 0 }}>
                    Страница в разработке
                </p>
            </div>
        </div>
    );
}