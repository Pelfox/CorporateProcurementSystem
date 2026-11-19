// Склонение товаров
export function pluralProducts(count) {
    const mod10 = count % 10;
    const mod100 = count % 100;
    let word;
    if (mod10 === 1 && mod100 !== 11) {
        word = 'товар';
    } else if (mod10 >= 2 && mod10 <= 4 && (mod100 < 10 || mod100 >= 20)) {
        word = 'товара';
    } else {
        word = 'товаров';
    }
    return count + ' ' + word;
}