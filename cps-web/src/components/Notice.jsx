export default function Notice({ children, success = false }) {
    if (!children) return null;
    return <div className={success ? 'notice notice-success' : 'notice notice-error'} role={success ? 'status' : 'alert'}>{children}</div>;
}
