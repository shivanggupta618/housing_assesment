export type Property = { square_footage:number; bedrooms:number; bathrooms:number; year_built:number; lot_size:number; distance_to_city_center:number; school_rating:number };
export type MarketRow = Property & { price:number };
export type Summary = { count:number; average:number; median:number; minimum:number; maximum:number; rows:MarketRow[] };
