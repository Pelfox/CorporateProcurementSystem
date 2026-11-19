import { Link } from 'react-router-dom';
export default function PageHeader({ title, children }) {
    return <div className="page-header"><div><Link to="/services" className="breadcrumb">Все сервисы</Link><h1 className="page-title">{title}</h1></div><div className="action-row">{children}</div></div>;
}
